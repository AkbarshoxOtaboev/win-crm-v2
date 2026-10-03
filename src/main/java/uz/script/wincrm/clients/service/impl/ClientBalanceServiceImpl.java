package uz.script.wincrm.clients.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.clients.Client;
import uz.script.wincrm.clients.ClientBalance;
import uz.script.wincrm.clients.dto.ClientBalanceDTO;
import uz.script.wincrm.clients.mapper.ClientBalanceMapper;
import uz.script.wincrm.clients.repository.ClientBalanceRepository;
import uz.script.wincrm.clients.repository.ClientRepository;
import uz.script.wincrm.clients.response.ClientBalanceResponse;
import uz.script.wincrm.clients.service.ClientBalanceService;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.CurrencyMath;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.payment.repository.PaymentRepository;
import uz.script.wincrm.sale.repository.SaleOrderRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClientBalanceServiceImpl implements ClientBalanceService {

    private final ClientBalanceRepository balanceRepository;
    private final ClientRepository clientRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ClientBalanceResponse> fetchAll(LocalDate fromDate, LocalDate toDate) {
        LocalDate resolvedFrom = resolveFromDate(fromDate);
        LocalDate resolvedTo = resolveToDate(toDate);

        // DIQQAT: har bir balans qatori uchun 2 tadan qo'shimcha SUM so'rovi bajariladi (N+1).
        return balanceRepository.findAll()
                .stream()
                .map(balance -> periodResponse(balance.getClient(), balance.getCurrency(), balance,
                        resolvedFrom, resolvedTo))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientBalanceResponse> findByClientId(Long clientId, LocalDate fromDate, LocalDate toDate) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));

        LocalDate resolvedFrom = resolveFromDate(fromDate);
        LocalDate resolvedTo = resolveToDate(toDate);
        List<ClientBalance> balances = balanceRepository.findAllByClient_IdOrderByCurrencyAsc(clientId);

        return currenciesOf(balances).stream()
                .map(currency -> periodResponse(client, currency,
                        balances.stream().filter(b -> b.getCurrency() == currency).findFirst().orElse(null),
                        resolvedFrom, resolvedTo))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientBalanceResponse> findAllTimeByClientId(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));

        List<ClientBalance> balances = balanceRepository.findAllByClient_IdOrderByCurrencyAsc(clientId);
        if (balances.isEmpty()) {
            return List.of(ClientBalanceResponse.builder()
                    .clientId(client.getId())
                    .clientFullName(client.getFullName())
                    .currency(Currency.BASE)
                    .totalPurchase(BigDecimal.ZERO)
                    .totalPaid(BigDecimal.ZERO)
                    .totalDebt(BigDecimal.ZERO)
                    .build());
        }
        return balances.stream().map(ClientBalanceMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public void recalculateClientBalance(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));

        List<ClientBalance> existing = balanceRepository.findAllByClient_IdOrderByCurrencyAsc(clientId);
        Set<Currency> currencies = EnumSet.of(Currency.BASE);
        existing.forEach(b -> currencies.add(b.getCurrency()));
        currencies.addAll(saleOrderRepository.findCurrenciesByClientId(clientId));
        currencies.addAll(paymentRepository.findDebtCurrenciesByClientId(clientId));

        for (Currency currency : currencies) {
            ClientBalance balance = existing.stream()
                    .filter(b -> b.getCurrency() == currency)
                    .findFirst()
                    .orElseGet(() -> ClientBalance.builder().client(client).currency(currency).build());

            BigDecimal totalPurchase = saleOrderRepository.sumTotalSumByClientIdAndCurrency(clientId, currency);
            BigDecimal totalPaid = paymentRepository.sumAppliedByClientIdAndCurrency(clientId, currency);

            balance.setTotalPurchase(totalPurchase);
            balance.setTotalPaid(totalPaid);
            balance.setTotalDebt(totalPurchase.subtract(totalPaid));
            balance.setLastUpdated(LocalDateTime.now());
            balanceRepository.save(balance);
        }
    }

    @Override
    @Transactional
    public ClientBalanceResponse adjustBalance(Long clientId, ClientBalanceDTO dto) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
        Currency currency = CurrencyMath.orBase(dto.getCurrency());

        ClientBalance balance = balanceRepository.findByClient_IdAndCurrency(clientId, currency)
                .orElseGet(() -> ClientBalance.builder().client(client).currency(currency).build());

        balance.setTotalPurchase(dto.getTotalPurchase());
        balance.setTotalPaid(dto.getTotalPaid());
        balance.setTotalDebt(dto.getTotalPurchase().subtract(dto.getTotalPaid()));
        balance.setLastUpdated(LocalDateTime.now());

        return ClientBalanceMapper.toResponse(balanceRepository.save(balance));
    }

    private ClientBalanceResponse periodResponse(Client client, Currency currency, ClientBalance balance,
                                                 LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(LocalTime.MAX);
        BigDecimal periodPurchase = saleOrderRepository.sumTotalSumByClientIdAndCurrencyAndDateRange(
                client.getId(), currency, fromDateTime, toDateTime);
        BigDecimal periodPaid = paymentRepository.sumAppliedByClientIdAndCurrencyAndDateRange(
                client.getId(), currency, fromDateTime, toDateTime);

        return ClientBalanceResponse.builder()
                .id(balance != null ? balance.getId() : null)
                .clientId(client.getId())
                .clientFullName(client.getFullName())
                .currency(currency)
                .totalPurchase(periodPurchase)
                .totalPaid(periodPaid)
                .totalDebt(periodPurchase.subtract(periodPaid))
                .lastUpdated(balance != null ? balance.getLastUpdated() : null)
                .periodFrom(from)
                .periodTo(to)
                .build();
    }

    private List<Currency> currenciesOf(List<ClientBalance> balances) {
        Set<Currency> set = EnumSet.of(Currency.BASE);
        balances.forEach(b -> set.add(b.getCurrency()));
        return set.stream().sorted(Comparator.naturalOrder()).toList();
    }

    private LocalDate resolveFromDate(LocalDate fromDate) {
        return fromDate != null ? fromDate : YearMonth.now().atDay(1);
    }

    private LocalDate resolveToDate(LocalDate toDate) {
        return toDate != null ? toDate : YearMonth.now().atEndOfMonth();
    }
}

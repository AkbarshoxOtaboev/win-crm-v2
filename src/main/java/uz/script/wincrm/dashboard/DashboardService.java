package uz.script.wincrm.dashboard;

import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.dashboard.responses.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Summalar {@code display} valyutasida: har bir hujjat o'z kursi bo'yicha (kurs bo'lmasa hujjat sanasidagi
 * kompaniya kursi bilan) o'giriladi, so'ng jamlanadi.
 */
public interface DashboardService {

    /**
     * Berilgan sana oralig'ida eng ko'p miqdorda sotilgan TOP 10 mahsulot.
     */
    List<TopGoodsResponse> fetchTopGoodsByQuantity(LocalDate startDate, LocalDate endDate, Currency display);

    /**
     * Berilgan sana oralig'ida eng ko'p summada sotilgan TOP 10 mahsulot.
     */
    List<TopGoodsResponse> fetchTopGoodsByAmount(LocalDate startDate, LocalDate endDate, Currency display);

    /**
     * Berilgan sana oralig'ida GoodsGroup (mahsulot guruhi) bo'yicha jamlangan statistika.
     */
    List<GoodsGroupSummaryResponse> fetchGoodsGroupSummary(LocalDate startDate, LocalDate endDate, Currency display);

    /**
     * Berilgan sana oralig'ida eng ko'p savdo qilgan TOP 10 sotuvchi (User),
     * umumiy savdo summasi bo'yicha kamayish tartibida.
     */
    List<TopSellerResponse> fetchTopSellers(LocalDate startDate, LocalDate endDate, Currency display);

    /**
     * Berilgan sana-vaqt oralig'ida (fromDate - toDate) barcha to'lovlarni PaymentType
     * bo'yicha jamlab qaytaradi. Har bir tur o'z kassa valyutasida - valyutalar qo'shilmaydi.
     */
    List<PaymentTypeSummaryResponse> fetchPaymentSummaryByType(LocalDateTime fromDate, LocalDateTime toDate);

    /**
     * Berilgan sana-vaqt oralig'idagi har bir kun uchun (to'lov bo'lmagan kunlar ham
     * 0 summa bilan) jami to'lov summasi ({@code display} da) va PaymentType bo'yicha taqsimotini qaytaradi.
     */
    List<DailyPaymentSummaryResponse> fetchDailyPayments(LocalDateTime fromDate, LocalDateTime toDate, Currency display);

    /**
     * Xarajatlar kategoriya bo'yicha, {@code display} valyutasida.
     */
    List<DailyExpenseReportResponse> fetchExpenseReport(LocalDate fromDate, LocalDate toDate, Currency display);

    /**
     * Har bir kassa (to'lov turi) o'z valyutasida: davr boshidagi qoldiq, kirim, chiqim, davr oxiridagi qoldiq.
     */
    List<CashBalanceResponse> fetchCashBalances(LocalDate fromDate, LocalDate toDate);

    /**
     * Xorijiy valyutadagi buyurtmalarga kelgan to'lovlar bo'yicha kurs farqi (so'mda).
     */
    FxDifferenceResponse fetchFxDifference(LocalDate fromDate, LocalDate toDate);

    /**
     * Sotilgan tovarlardan foyda (bekor qilinmagan buyurtmalar, buyurtma sanasi bo'yicha):
     * tushum (chegirmadan keyin, yetkazishsiz) - tannarx; kunlar, mahsulotlar va sotuvchilar kesimida.
     */
    ProfitReportResponse fetchProfitReport(LocalDate fromDate, LocalDate toDate, Currency display);
}

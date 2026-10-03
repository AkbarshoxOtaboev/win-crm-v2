package uz.script.wincrm.currency;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import uz.script.wincrm.utils.BaseEntity;
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Kunlik kompaniya kursi: 1 birlik valyuta = rate so'm. Kun uchun kurs bo'lmasa, oxirgi oldingi kurs amal qiladi. */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(
        name = TableName.EXCHANGE_RATES,
        uniqueConstraints = @UniqueConstraint(name = "uk_exchange_rate_currency_date", columnNames = {"currency", "rate_date"})
)
public class ExchangeRate extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ExchangeRateSource source;
}

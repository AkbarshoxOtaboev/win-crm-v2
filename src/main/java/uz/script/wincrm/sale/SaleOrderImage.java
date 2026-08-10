package uz.script.wincrm.sale;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.sale.enums.SaleOrderImageType;
import uz.script.wincrm.utils.BaseEntity;
import uz.script.wincrm.utils.TableName;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(name = TableName.SALE_ORDER_IMAGES)
@SQLRestriction("status <> 'DELETED'")
public class SaleOrderImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_order_id", nullable = false)
    private SaleOrder saleOrder;

    /**
     * StorageService.uploadFile() qaytargan saqlangan fayl nomi (UUID.ext).
     * Yuklab olish: GET /api/files/{fileName}.
     */
    @Column(nullable = false)
    private String fileName;

    /**
     * Foydalanuvchi yuklagan asl fayl nomi (masalan "obyekt-rasmi.jpg") — ko'rsatish uchun.
     */
    private String originalFileName;

    /**
     * MIME turi (masalan "image/jpeg") — ixtiyoriy, ko'rsatish/validatsiya uchun.
     */
    private String contentType;

    /**
     * Fayl hajmi (bayt) — ixtiyoriy, ko'rsatish uchun.
     */
    private Long size;

    /**
     * Rasm turi/kategoriyasi (obyekt rasmi yoki loyiha/chizma). Ixtiyoriy.
     */
    @Enumerated(EnumType.STRING)
    private SaleOrderImageType imageType;
}
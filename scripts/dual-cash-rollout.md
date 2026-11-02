# Ikki kassa — chiqarish yo‘riqnomasi

## Model

| Kassa | Kim | Kirim | Chiqim |
|-------|-----|-------|--------|
| Operatsion | Kassir | Mijoz to‘lovlari | Topshirish (PENDING/ACCEPTED), operatsion xarajat/to‘lov |
| Rahbar (VAULT) | ADMIN / DIRECTOR / SUPER_ADMIN | ACCEPTED topshirish | Rahbar xarajati / yetkazib beruvchiga to‘lov |

PENDING summa hech kim tomonidan sarflanmaydi.

## Migratsiya

1. Backendni ishga tushiring (`ddl-auto=update` `cash_source` ustunini qo‘shadi).
2. `CashSourceSchemaMigrator` eski `expenses` / `supplier_payments` qatorlarini `OPERATIONAL` qiladi.
3. Stagingda 1 filial bilan sinang (pastdagi checklist).
4. Prodga chiqaring.

## Filial checklist

- [ ] Kassir topshirdi → Operatsion kamaydi, status PENDING
- [ ] Admin qabul qildi → Rahbar oshdi
- [ ] Kassir Rahbar (VAULT) dan yozolmaydi
- [ ] Admin Rahbarda 0 bo‘lsa xarajat/to‘lov rad etiladi
- [ ] Admin VAULT to‘lovi → faqat Rahbar kamayadi
- [ ] Reject → pul Operatsionga qaytadi
- [ ] 2 filial parallel: birining topshirig‘i ikkinchisiga ta’sir qilmasin

## Foydalanuvchiga

1. Kassir: kun yakunida `/cash-handovers` orqali Operatsion qoldiqni topshiradi.
2. Admin/Direktor: topshirishni qabul qiladi.
3. Admin/Direktor: xarajat va yetkazib beruvchi to‘lovida **Rahbar** manbasini tanlaydi (default).
4. Kassada pul bo‘lmasa — saqlash bloklanadi.

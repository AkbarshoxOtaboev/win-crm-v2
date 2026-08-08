package uz.script.wincrm.salary.service;

import uz.script.wincrm.salary.response.SalarySlipResponse;

public interface SalarySlipService {

    /** Bir xodimning bitta oy (yil+oy) uchun yig'ma oylik hisob-kitobini qaytaradi. */
    SalarySlipResponse getSlip(Long userId, Integer year, Integer month);
}

package uz.script.wincrm.filial;

import java.util.List;

public interface FilialService {
    FilialResponse create(FilialDTO dto);

    FilialResponse update(Long id, FilialDTO dto);

    void delete(Long id);

    FilialResponse findById(Long id);

    List<FilialResponse> fetchAll();

    FilialResponse assignDirector(Long filialId, Long directorId);
}

package com.ekko.transport_api.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ekko.transport_api.data.dto.student.StudentListData;
import com.ekko.transport_api.infra.exception.ErrorDetailsException;
import com.ekko.transport_api.infra.exception.NotFoundException;
import com.ekko.transport_api.models.Institution;
import com.ekko.transport_api.models.StudentModel;
import com.ekko.transport_api.models.User;
import com.ekko.transport_api.repositories.StudentRepository;

@Service
public class StudentService {

    @Autowired
    private StudentRepository repository;

    public Page<StudentListData> listAll(Pageable pageable) {
        Institution institution = getCurrentInstitution();

        if (isMasterInstitution(institution)) {
            return repository.findAll(pageable).map(StudentListData::new);
        }

        return repository.findByInstitutionId(institution.getId(), pageable)
                .map(StudentListData::new);
    }

    public StudentListData findByQrCodeIdentifier(String readCodeStudent) {
        Institution institution = getCurrentInstitution();

        Optional<StudentModel> student = isMasterInstitution(institution) ?
                repository.findByQrCodeIdentifier(readCodeStudent) :
                repository.findByQrCodeIdentifierAndInstitutionId(readCodeStudent, institution.getId());

        return student.map(StudentListData::new)
                .orElseThrow(() -> new NotFoundException("Não existe um aluno com esse código."));
    }

    private boolean isMasterInstitution(Institution institution) {
        return institution != null && institution.getId().equals(1L);
    }

    private Institution getCurrentInstitution() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ErrorDetailsException("Nenhum usuário autenticado encontrado.");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            throw new ErrorDetailsException("Usuário autenticado inválido.");
        }
        User user = (User) principal;
        if (user.getInstitution() == null) {
            throw new ErrorDetailsException("Usuário sem instituição associada.");
        }
        return user.getInstitution();
    }
}

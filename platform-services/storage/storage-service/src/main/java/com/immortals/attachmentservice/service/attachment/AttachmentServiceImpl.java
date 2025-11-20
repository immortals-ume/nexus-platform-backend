package com.immortals.attachmentservice.service.attachment;


import com.immortals.attachmentservice.model.entity.Attachment;
import com.immortals.attachmentservice.model.payload.attachment.AttachmentPayload;
import com.immortals.attachmentservice.repository.AttachmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AttachmentServiceImpl implements AttachmentService{

    private final AttachmentRepository attachmentRepository;

    @Autowired
    public AttachmentServiceImpl(AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }


    @Override
    public String saveMetadata(Attachment attachment) {
        return "";
    }

    @Override
    public List<Attachment> getAttachmentPerUser(Long userId) {
        return List.of();
    }

    @Override
    public List<Attachment> getAttachments() {
        return List.of();
    }

    @Override
    public String updateMetadata(Long attachmentId, AttachmentPayload attachmentPayload) {
        return "";
    }

    @Override
    public String deleteMetadata(Long attachmentId) {
        return "";
    }
}

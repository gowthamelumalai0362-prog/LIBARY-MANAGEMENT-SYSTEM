package com.library.lms.dto;

import com.library.lms.entity.BookCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponseDto {
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private BookCategory category;
    private String publisher;
    private Integer publicationYear;
    private Integer totalCopies;
    private Integer availableCopies;
    private String description;
    private String coverImageUrl;
    private List<BookCopyDto> copies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

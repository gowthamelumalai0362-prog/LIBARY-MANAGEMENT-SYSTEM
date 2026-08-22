package com.library.lms.dto;

import com.library.lms.entity.BookCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCreateDto {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Author is required")
    private String author;

    @NotBlank(message = "ISBN is required")
    private String isbn;

    @NotNull(message = "Category is required")
    private BookCategory category;

    private String publisher;

    private Integer publicationYear;

    private String description;

    private String coverImageUrl;

    @Min(value = 1, message = "Initial copies count must be at least 1")
    @Builder.Default
    private Integer initialCopiesCount = 1;
}

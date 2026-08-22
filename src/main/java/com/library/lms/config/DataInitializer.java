package com.library.lms.config;

import com.library.lms.entity.*;
import com.library.lms.repository.*;
import com.library.lms.service.LibraryPolicyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;
    private final LibraryPolicyService policyService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing LMS seed data...");

        // 1. Initialize Default Library Policy
        policyService.getOrCreatePolicy();

        // 2. Initialize Default Users if not present
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .firstName("Eleanor")
                    .lastName("Vance")
                    .email("admin@library.com")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .phone("+1-555-0101")
                    .build();

            User librarian = User.builder()
                    .firstName("Arthur")
                    .lastName("Pendleton")
                    .email("librarian@library.com")
                    .passwordHash(passwordEncoder.encode("Librarian@123"))
                    .role(Role.LIBRARIAN)
                    .status(UserStatus.ACTIVE)
                    .phone("+1-555-0102")
                    .build();

            User member1 = User.builder()
                    .firstName("Sophia")
                    .lastName("Rodriguez")
                    .email("member@library.com")
                    .passwordHash(passwordEncoder.encode("Member@123"))
                    .role(Role.MEMBER)
                    .status(UserStatus.ACTIVE)
                    .phone("+1-555-0103")
                    .build();

            User member2 = User.builder()
                    .firstName("David")
                    .lastName("Kim")
                    .email("david@library.com")
                    .passwordHash(passwordEncoder.encode("Member@123"))
                    .role(Role.MEMBER)
                    .status(UserStatus.ACTIVE)
                    .phone("+1-555-0104")
                    .build();

            User member3 = User.builder()
                    .firstName("Emily")
                    .lastName("Watson")
                    .email("emily@library.com")
                    .passwordHash(passwordEncoder.encode("Member@123"))
                    .role(Role.MEMBER)
                    .status(UserStatus.ACTIVE)
                    .phone("+1-555-0105")
                    .build();

            userRepository.saveAll(List.of(admin, librarian, member1, member2, member3));
            log.info("Seeded initial users (admin, librarian, members).");
        }

        // 3. Initialize Catalog Books and Physical Copies
        if (bookRepository.count() == 0) {
            seedBookCatalog();
        }

        log.info("LMS Seed Data Initialized successfully.");
    }

    private void seedBookCatalog() {
        List<BookSeedData> seedDataList = List.of(
                new BookSeedData(
                        "Clean Code: A Handbook of Agile Software Craftsmanship",
                        "Robert C. Martin",
                        "9780132350884",
                        BookCategory.SOFTWARE_ENGINEERING,
                        "Prentice Hall",
                        2008,
                        "Even bad code can function. But if code isn't clean, it can bring a development organization to its knees. Every year, countless hours and significant resources are lost because of poorly written code.",
                        "https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=500&auto=format&fit=crop&q=60",
                        4
                ),
                new BookSeedData(
                        "Designing Data-Intensive Applications",
                        "Martin Kleppmann",
                        "9781449373320",
                        BookCategory.COMPUTER_SCIENCE,
                        "O'Reilly Media",
                        2017,
                        "Data is at the center of many challenges in system design today. Difficult issues need to be figured out, such as scalability, consistency, reliability, efficiency, and maintainability.",
                        "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500&auto=format&fit=crop&q=60",
                        3
                ),
                new BookSeedData(
                        "Artificial Intelligence: A Modern Approach",
                        "Stuart Russell, Peter Norvig",
                        "9780134610993",
                        BookCategory.ARTIFICIAL_INTELLIGENCE,
                        "Pearson",
                        2020,
                        "The most comprehensive, up-to-date introduction to the theory and practice of artificial intelligence, covering probabilistic reasoning, deep learning, and multiagent systems.",
                        "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=500&auto=format&fit=crop&q=60",
                        3
                ),
                new BookSeedData(
                        "Effective Java (3rd Edition)",
                        "Joshua Bloch",
                        "9780134685991",
                        BookCategory.SOFTWARE_ENGINEERING,
                        "Addison-Wesley",
                        2018,
                        "The definitive guide to best-practices in Java programming. Highly recommended for every developer seeking mastery over modern idioms and patterns.",
                        "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500&auto=format&fit=crop&q=60",
                        4
                ),
                new BookSeedData(
                        "Introduction to Algorithms (CLRS)",
                        "Thomas H. Cormen, Charles E. Leiserson, Ronald L. Rivest, Clifford Stein",
                        "9780262033848",
                        BookCategory.COMPUTER_SCIENCE,
                        "MIT Press",
                        2009,
                        "A comprehensive textbook on computer algorithms, covering a broad range of algorithms in depth, yet making their design and analysis accessible to all levels of readers.",
                        "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=500&auto=format&fit=crop&q=60",
                        2
                ),
                new BookSeedData(
                        "Deep Learning",
                        "Ian Goodfellow, Yoshua Bengio, Aaron Courville",
                        "9780262035613",
                        BookCategory.DATA_SCIENCE,
                        "MIT Press",
                        2016,
                        "An introduction to a broad range of topics in deep learning, covering mathematical and conceptual background, deep learning techniques used in industry, and research perspectives.",
                        "https://images.unsplash.com/photo-1509228468518-180dd4864904?w=500&auto=format&fit=crop&q=60",
                        3
                ),
                new BookSeedData(
                        "The Pragmatic Programmer: Your Journey to Mastery",
                        "David Thomas, Andrew Hunt",
                        "9780135957059",
                        BookCategory.SOFTWARE_ENGINEERING,
                        "Addison-Wesley",
                        2019,
                        "Straight from the programming trenches, The Pragmatic Programmer cuts through the increasing specialization and technicalities of modern software development.",
                        "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500&auto=format&fit=crop&q=60",
                        3
                ),
                new BookSeedData(
                        "Sapiens: A Brief History of Humankind",
                        "Yuval Noah Harari",
                        "9780062316097",
                        BookCategory.HISTORY,
                        "Harper",
                        2015,
                        "From a renowned historian comes a groundbreaking narrative of humanity’s creation and evolution—a #1 international bestseller that explores the ways in which biology and history define us.",
                        "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500&auto=format&fit=crop&q=60",
                        3
                ),
                new BookSeedData(
                        "Principles: Life and Work",
                        "Ray Dalio",
                        "9781501124020",
                        BookCategory.BUSINESS,
                        "Simon & Schuster",
                        2017,
                        "Ray Dalio shares the unconventional principles that he’s developed, refined, and used over the past forty years to create unique results in both life and business.",
                        "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=500&auto=format&fit=crop&q=60",
                        2
                ),
                new BookSeedData(
                        "To Kill a Mockingbird",
                        "Harper Lee",
                        "9780061120084",
                        BookCategory.LITERATURE,
                        "J.B. Lippincott & Co.",
                        1960,
                        "The unforgettable novel of a childhood in a sleepy Southern town and the crisis of conscience that rocked it, compassionate and dramatic.",
                        "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500&auto=format&fit=crop&q=60",
                        3
                )
        );

        List<Book> createdBooks = new ArrayList<>();
        for (BookSeedData data : seedDataList) {
            Book book = Book.builder()
                    .title(data.title)
                    .author(data.author)
                    .isbn(data.isbn)
                    .category(data.category)
                    .publisher(data.publisher)
                    .publicationYear(data.year)
                    .description(data.description)
                    .coverImageUrl(data.coverUrl)
                    .copies(new ArrayList<>())
                    .build();

            Book savedBook = bookRepository.save(book);

            String cleanIsbn = data.isbn.replaceAll("[^a-zA-Z0-9]", "");
            for (int i = 1; i <= data.copyCount; i++) {
                BookCopy copy = BookCopy.builder()
                        .book(savedBook)
                        .barcode(String.format("BC-%s-%02d", cleanIsbn, i))
                        .status(CopyStatus.AVAILABLE)
                        .acquiredDate(LocalDate.now().minusMonths(2))
                        .conditionNotes("Good condition")
                        .build();
                savedBook.addCopy(copy);
            }

            savedBook = bookRepository.save(savedBook);
            createdBooks.add(savedBook);
        }

        // Seed some realistic circulation activity for demo
        User member1 = userRepository.findByEmail("member@library.com").orElse(null);
        User member2 = userRepository.findByEmail("david@library.com").orElse(null);

        if (member1 != null && member2 != null && !createdBooks.isEmpty()) {
            Book cleanCode = createdBooks.get(0);
            Book dda = createdBooks.get(1);

            // Active loan for member1
            if (!cleanCode.getCopies().isEmpty()) {
                BookCopy copy1 = cleanCode.getCopies().get(0);
                copy1.setStatus(CopyStatus.BORROWED);
                bookCopyRepository.save(copy1);
                cleanCode.recalculateAvailableCopies();
                bookRepository.save(cleanCode);

                BorrowRecord record1 = BorrowRecord.builder()
                        .user(member1)
                        .bookCopy(copy1)
                        .borrowDate(LocalDate.now().minusDays(5))
                        .dueDate(LocalDate.now().plusDays(9))
                        .status(BorrowStatus.ACTIVE)
                        .renewalCount(0)
                        .build();
                borrowRecordRepository.save(record1);
            }

            // Overdue loan with fine for member2
            if (cleanCode.getCopies().size() > 1) {
                BookCopy copy2 = cleanCode.getCopies().get(1);
                copy2.setStatus(CopyStatus.BORROWED);
                bookCopyRepository.save(copy2);
                cleanCode.recalculateAvailableCopies();
                bookRepository.save(cleanCode);

                BorrowRecord overdueRecord = BorrowRecord.builder()
                        .user(member2)
                        .bookCopy(copy2)
                        .borrowDate(LocalDate.now().minusDays(20))
                        .dueDate(LocalDate.now().minusDays(6)) // 6 days overdue
                        .status(BorrowStatus.OVERDUE)
                        .renewalCount(0)
                        .build();
                BorrowRecord savedOverdue = borrowRecordRepository.save(overdueRecord);

                Fine fine = Fine.builder()
                        .borrowRecord(savedOverdue)
                        .user(member2)
                        .amount(new BigDecimal("3.00")) // 6 days * $0.50
                        .status(FineStatus.UNPAID)
                        .reason("Overdue by 6 day(s) at $0.50/day")
                        .build();
                fineRepository.save(fine);
            }

            // Reservation queue for DDA
            Reservation res1 = Reservation.builder()
                    .user(member1)
                    .book(dda)
                    .status(ReservationStatus.PENDING)
                    .notificationSent(false)
                    .build();
            reservationRepository.save(res1);
        }
    }

    private static class BookSeedData {
        String title;
        String author;
        String isbn;
        BookCategory category;
        String publisher;
        int year;
        String description;
        String coverUrl;
        int copyCount;

        public BookSeedData(String title, String author, String isbn, BookCategory category, String publisher, int year, String description, String coverUrl, int copyCount) {
            this.title = title;
            this.author = author;
            this.isbn = isbn;
            this.category = category;
            this.publisher = publisher;
            this.year = year;
            this.description = description;
            this.coverUrl = coverUrl;
            this.copyCount = copyCount;
        }
    }
}

# Лаборатори №8: Mock объектууд — Mockito

**F.CSA313 — Программ хангамжийн чанарын баталгаа ба тест (2026)**

- **Оюутны нэр:** Б.Билгүүнтөр
- **Оюутны код:** B242270505

```
$ java -version
openjdk version "26.0.1" 2026-04-21
OpenJDK Runtime Environment (build 26.0.1+8-34)
OpenJDK 64-Bit Server VM (build 26.0.1+8-34, mixed mode, sharing)

$ mvn -version
Apache Maven 3.10.1 (3afd8084b5629f80b97750906a719879ed4cd283)
Maven home: /opt/homebrew/Cellar/maven/3.10.1/libexec
Java version: 27.0.1.1, vendor: Homebrew, runtime: /opt/homebrew/Cellar/openjdk/27.0.1.1/libexec/openjdk.jdk/Contents/Home
Default locale: en_MN, platform encoding: UTF-8, time zone: Asia/Ulaanbaatar
OS name: "mac os x", version: "26.6.2", arch: "aarch64", family: "mac"
```

> Терминалын `java` нь 26, харин Maven Homebrew-ийн JDK 27-г ашигладаг. `pom.xml`-д `release 17` тавьсан. JDK 21+ дээр гардаг byte-buddy agent-ийн анхааруулга бол хэвийн үзэгдэл.

## Төсөл

Starter нь Teams Lab08-ийн `starter.zip` (pom.xml + src/main), өөрчлөөгүй. Тестүүд: [CourseRegistrationServiceTest.java](src/test/java/mn/edu/must/sqat/CourseRegistrationServiceTest.java). `StudentRepository` болон `EmailSender`-ийг `@Mock`-оор орлуулсан.

| № | Тест (юуг_ямарНөхцөлд_юуХүлээгдэнэ) | Хэрэгсэл |
|---|---|---|
| A1 | `register_activeStudentWithPrereqs_returnsOkSavesAndSendsEmail` | `when` + `verify` (`eq`/`anyString`/`contains`) |
| A2 | `register_unknownStudent_returnsNoStudentAndSavesNothing` | `verify(..., never())` |
| A3 | `register_inactiveStudent_returnsInactiveAndNoEmailInteraction` | `verifyNoInteractions` |
| A4 | `register_missingPrerequisite_returnsPrereqErrorAndSavesNothing` | `when` + `never` |
| A5 | `register_emailSenderThrows_stillReturnsOkAndSaved` | `doThrow(...).when(...)` |
| A6 | `register_nullArgument_throwsAndTouchesNoDependency` | `assertThrows` + `verifyNoInteractions` |
| A7 | `register_success_emailBodyContainsCourseCode` | `ArgumentCaptor` |
| A8 | `register_errorClass_returnsMatchingErrorAndSavesNothing` (3 мөр) | `@ParameterizedTest` + `@CsvSource` + `lenient()` |

Matcher-ийн дүрэм: нэг дуудалтад matcher хэрэглэсэн бол бүх аргументад matcher ашигласан. Жишээ нь `send(eq(...), anyString(), contains("CS313"))`, captor-той үед `send(to.capture(), anyString(), body.capture())`.

## Тестийн тоо

[results/mvn-test.txt](results/mvn-test.txt): **`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0` — BUILD SUCCESS**. 7 энгийн тест дээр A8-ийн 3 мөр нэмэгдэж нийт 10 болно.

**Яагаад A8-д `lenient()` хэрэгтэй вэ?** A8-ийн мөр бүр өөр замаар гардаг. `NO_STUDENT` болон `INACTIVE` мөрүүдэд `courseExists` огт дуудагдахгүй тул энэ stub ашиглагдахгүй. `MockitoExtension`-ий strict stubs горим ашиглагдаагүй stub-ийг `UnnecessaryStubbingException` болгож тестийг унагадаг. Иймд мөр бүрд хэрэгтэй байж болох stub-уудыг `lenient()` болгосон.

## Хэсэг B: мини мутаци

| Мутант | Өөрчлөлт | Унасан тест | Мессеж | Файл |
|---|---|---|---|---|
| 1 | `if (!student.active()) ...`-ийг комментолсон | **A3** `register_inactiveStudent_...`, **A8 [INACTIVE]** мөр | A3: `expected <ERROR_INACTIVE_STUDENT> but was <ERROR_NO_COURSE>`. A8: `... but was <OK>` | [mutant-1](results/mvn-test-mutant-1.txt): Failures 2, BUILD FAILURE |
| 2 | `repository.saveRegistration(...)`-ийг комментолсон | **A1** `register_activeStudentWithPrereqs_...`, **A5** `register_emailSenderThrows_...` | `Wanted but not invoked: repository.saveRegistration("B001", "CS313")` | [mutant-2](results/mvn-test-mutant-2.txt): Failures 2, BUILD FAILURE |
| 3 | `\|\| courseId == null`-ийг устгасан | **(засахаас өмнө) нэг ч тест унаагүй.** Засварын дараа **A6** `register_nullArgument_...` | `Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.` | Өмнө: [mutant-3-before-fix](results/mvn-test-mutant-3-before-fix.txt) (10/10 ногоон, BUILD SUCCESS). Дараа: [mutant-3](results/mvn-test-mutant-3.txt): Failures 1, BUILD FAILURE |

Гурван мутантад `UnnecessaryStubbingException` (Errors) гараагүй, бүх уналт Failures-д тоологдсон.

**Мутант 3-ын цоорхой ба засвар.** Эхний A6 зөвхөн `register(null, "CS313")`-ийг шалгадаг байсан. `studentId == null` хэвээр үлдсэн тул мутант амьд гарсан. Тиймээс A6-д `register("B001", null)`-ийг тусад нь нэмсэн. Мутант дээр `courseId=null` үед exception шидэгдэхгүй тул A6 унаж, мутант баригдсан.

**Сонирхолтой ажиглалт (мутант 1, A8 INACTIVE).** Mock-ийн `prerequisites()` stub-гүй үед хоосон жагсаалт буцаадаг. Тиймээс мутант дээр идэвхгүй оюутан "OK" авч бүртгэгдсэн. Mock-ийн default утга (null/false/хоосон) логикийг "чимээгүй" өөр зам руу оруулж болохыг харуулж байна.

Мутаци бүрийн дараа `git checkout`-оор `src/main`-ийг сэргээсэн. Эцсийн `mvn-test.txt` 10/10 ногоон.

## Дүгнэлт

Mock нь Mockito-гоор ажиллах үед тохируулдаг орлуулагч бөгөөд **харилцааг** (ямар метод, ямар аргументтай, хэдэн удаа дуудагдсан) шалгаж чаддаг. Харин гараар бичсэн fake нь энгийн бодит хэрэгжүүлэлт (жишээ нь `HashMap`-д хадгалдаг repository) бөгөөд ихэвчлэн **төлөвийг** шалгахад тохирдог. Fake-ийг олон тестэд дахин ашиглаж болох ч өөрөө алдаатай байж болзошгүй. Mock нь тест бүрд тусдаа, хурдан бөгөөд алдаа дуурайлгахад (`doThrow`) хялбар. Mock хийх ёсгүй зүйлс: тестлэгдэж буй класс өөрөө, `Student` гэх мэт энгийн өгөгдлийн объект (record), стандарт сан (`List`) болон өөрийн эзэмшдэггүй гуравдагч талын төрлүүд. Mock нь зөвхөн бидний тодорхойлсон интерфейсийн хил дээр л утгатай. Verify-г хэтрүүлбэл тест хэрэгжүүлэлтэд хэт уягдаж, үр дүн нь ижил хэвээр байхад ч дотоод дуудлагын дараалал өөрчлөгдөхөд унадаг хэврэг тест болно. Тиймээс бид зөвхөн гаднаас ажиглагдах ёстой нөлөөг (хадгалсан, и-мэйл явсан/яваагүй) л verify хийсэн. Мутацийн туршилт энэ тэнцвэрийг шалгасан: verify-гүй байсан бол мутант 2 (хадгалалт алга) баригдахгүй байсан. Харин хангалтгүй assertion (A6) нь мутант 3-ыг амьд үлдээсэн.

## Нэмэлт: AI өнцөг

Тестүүдийг AI туслах (Claude) ашиглан бичсэн бөгөөд Хэсэг B-ээр өөрсдөө эвдэж шалгасан. Эхний хувилбарын A6 нь зааврын жишээ шиг зөвхөн нэг null-ийг шалгасан тул мутант 3 амьд үлдсэн. Энэ нь AI-ийн "бүрэн мэт" харагдах тест ч сул assertion-тай байж болохыг харуулж байна. Мутацийн гаралтыг уншиж, A6-г засаж байж барьдаг болсон.

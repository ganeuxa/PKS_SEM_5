package ru.mirea.autosalon.service;

import ru.mirea.autosalon.model.Car;
import ru.mirea.autosalon.model.CarStatus;
import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.model.TestDriveStatistics;
import ru.mirea.autosalon.model.TestDriveStatus;
import ru.mirea.autosalon.repository.CarRepository;
import ru.mirea.autosalon.repository.ClientRepository;
import ru.mirea.autosalon.repository.TestDriveRequestRepository;
import ru.mirea.autosalon.util.Formats;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Расчёт статистики автосалона с помощью Stream API и коллекций (Map, EnumMap, LinkedHashMap).
 */
public class StatisticsService {

    private final ClientRepository clientRepository;
    private final CarRepository carRepository;
    private final TestDriveRequestRepository requestRepository;
    private final Clock clock;

    public StatisticsService(ClientRepository clientRepository, CarRepository carRepository,
                             TestDriveRequestRepository requestRepository, Clock clock) {
        this.clientRepository = clientRepository;
        this.carRepository = carRepository;
        this.requestRepository = requestRepository;
        this.clock = clock;
    }

    /** Загружает актуальные данные из БД и считает статистику. */
    public TestDriveStatistics collect() {
        return calculate(clientRepository.findAll(), carRepository.findAll(), requestRepository.findAll());
    }

    public TestDriveStatistics calculate(List<Client> clients, List<Car> cars, List<TestDriveRequest> requests) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        // EnumMap хранит ключи в порядке объявления констант enum; заранее заполняем нулями все статусы
        Map<TestDriveStatus, Long> requestsByStatus = new EnumMap<>(TestDriveStatus.class);
        for (TestDriveStatus status : TestDriveStatus.values()) {
            requestsByStatus.put(status, 0L);
        }
        requests.forEach(request -> requestsByStatus.merge(request.getStatus(), 1L, Long::sum));

        Map<CarStatus, Long> carsByStatus = cars.stream()
                .collect(Collectors.groupingBy(Car::getStatus, () -> new EnumMap<>(CarStatus.class),
                        Collectors.counting()));

        long activeRequests = requests.stream().filter(TestDriveRequest::isActive).count();
        long upcomingRequests = requests.stream()
                .filter(request -> request.isActive() && request.getScheduledAt().isAfter(now))
                .count();
        long todayRequests = requests.stream()
                .filter(request -> request.isActive() && request.getScheduledAt().toLocalDate().equals(today))
                .count();

        long completed = requestsByStatus.get(TestDriveStatus.COMPLETED);
        long closed = completed + requestsByStatus.get(TestDriveStatus.CANCELLED)
                + requestsByStatus.get(TestDriveStatus.NO_SHOW);
        double completionRate = closed == 0 ? 0 : completed * 100.0 / closed;
        double averageDuration = requests.stream()
                .mapToInt(TestDriveRequest::getDurationMinutes)
                .average()
                .orElse(0);
        double averageClientAge = clients.stream()
                .mapToInt(client -> client.getAge(today))
                .average()
                .orElse(0);

        Map<String, Object> indicators = new LinkedHashMap<>();
        indicators.put("Всего клиентов", clients.size());
        indicators.put("Средний возраст клиентов, лет", averageClientAge);
        indicators.put("Всего автомобилей", cars.size());
        indicators.put("Автомобилей в наличии", carsByStatus.getOrDefault(CarStatus.AVAILABLE, 0L));
        indicators.put("Автомобилей на обслуживании", carsByStatus.getOrDefault(CarStatus.IN_SERVICE, 0L));
        indicators.put("Продано автомобилей", carsByStatus.getOrDefault(CarStatus.SOLD, 0L));
        indicators.put("Всего заявок на тест-драйв", requests.size());
        indicators.put("Активных заявок (новые и подтверждённые)", activeRequests);
        indicators.put("Новых заявок", requestsByStatus.get(TestDriveStatus.NEW));
        indicators.put("Подтверждённых заявок", requestsByStatus.get(TestDriveStatus.CONFIRMED));
        indicators.put("Завершённых тест-драйвов", completed);
        indicators.put("Отменённых заявок", requestsByStatus.get(TestDriveStatus.CANCELLED));
        indicators.put("Неявок клиентов", requestsByStatus.get(TestDriveStatus.NO_SHOW));
        indicators.put("Предстоящих тест-драйвов", upcomingRequests);
        indicators.put("Тест-драйвов на сегодня", todayRequests);
        indicators.put("Доля состоявшихся тест-драйвов среди закрытых заявок, %", completionRate);
        indicators.put("Средняя длительность тест-драйва, мин", averageDuration);
        indicators.put("Самый популярный автомобиль", mostFrequent(requests, TestDriveRequest::getCar,
                Car::getTitle));
        indicators.put("Самый активный клиент", mostFrequent(requests, TestDriveRequest::getClient,
                Client::getFullName));

        return new TestDriveStatistics(now, indicators, requestsByStatus, countByBrand(requests));
    }

    /** Число заявок по маркам автомобилей, от самой популярной марки к наименее популярной. */
    private Map<String, Long> countByBrand(List<TestDriveRequest> requests) {
        return requests.stream()
                .collect(Collectors.groupingBy(request -> request.getCar().getBrand(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (first, second) -> first, LinkedHashMap::new));
    }

    /**
     * Находит объект (автомобиль или клиента), который чаще всего встречается в заявках.
     * Группировка идёт по самому объекту: equals/hashCode сущностей сравнивают ID.
     */
    private <T> String mostFrequent(List<TestDriveRequest> requests, Function<TestDriveRequest, T> key,
                                    Function<T, String> name) {
        return requests.stream()
                .collect(Collectors.groupingBy(key, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.<T, Long>comparingByValue()
                        .thenComparing(entry -> name.apply(entry.getKey()), Comparator.reverseOrder()))
                .map(entry -> name.apply(entry.getKey()) + " (" + entry.getValue() + " "
                        + Formats.plural(entry.getValue(), "заявка", "заявки", "заявок") + ")")
                .orElse("-");
    }
}

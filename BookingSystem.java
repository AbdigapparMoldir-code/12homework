import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
class BookingRequest {
    String clientName;
    LocalDate date;
    String venue;
    BookingRequest(String clientName, LocalDate date, String venue) {
        this.clientName = clientName; this.date = date; this.venue = venue;
    }
}
class PaymentGateway {
    boolean processPrepayment(String client, double amount) {
        System.out.println("[PaymentGateway] Обработка платежа " + client + " сумма: " + amount);
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        boolean ok = new Random().nextInt(100) < 80;
        System.out.println("[PaymentGateway] Платеж " + (ok ? "успешен" : "отклонён"));
        return ok;
    }
}
class Venue {
    String name;
    Set<LocalDate> bookedDates = ConcurrentHashMap.newKeySet();
    Venue(String name) { this.name = name; }

    boolean isAvailable(LocalDate d) {
        boolean available = !bookedDates.contains(d);
        System.out.println("[System] Проверка доступности для " + name + " на " + d + " -> " + (available ? "Доступна" : "Занята"));
        return available;
    }
    void confirmBooking(LocalDate d) { bookedDates.add(d); System.out.println("[Venue] Дата забронирована: " + d); }
}
class VenueAdmin {
    String name;
    VenueAdmin(String name){ this.name = name; }
    void prepareTasks(BookingRequest req, List<String> tasks) {
        System.out.println("[" + name + "] Подготовлен список задач для мероприятия: " + tasks);
    }
}
class Contractor {
    String name;
    Contractor(String name){ this.name = name; }
    void notifyTask(String task) {
        System.out.println("[Подрядчик " + name + "] Получил задачу: " + task);
    }
    boolean confirmTask(String task) {
        System.out.println("[Подрядчик " + name + "] Подтверждает выполнение: " + task);
        return true;
    }
}
public class BookingSystem {
    public static void main(String[] args) throws InterruptedException {
        PaymentGateway pg = new PaymentGateway();
        Venue hall = new Venue("Большой зал");
        VenueAdmin admin = new VenueAdmin("AdminA");
        Contractor c1 = new Contractor("Decors Inc.");
        Contractor c2 = new Contractor("CateringCo");
        Contractor c3 = new Contractor("Sound&Light");
        LocalDate desiredDate = LocalDate.of(2025, 11, 20);
        BookingRequest req = new BookingRequest("Client1", desiredDate, hall.name);
        System.out.println("[Client] Запрос на дату: " + req.date + " зал: " + req.venue);
        boolean available = hall.isAvailable(req.date);
        if (!available) {
            System.out.println("[System] Предложить иную дату/зала клиенту.");
            return;
        }
        System.out.println("[System] Площадка доступна. Информация о стоимости отправлена клиенту.");
        System.out.println("[Client] Подтверждаю бронирование и отправляю предоплату.");
        boolean paymentOk = pg.processPrepayment(req.clientName, 1000.0);
        if (!paymentOk) {
            System.out.println("[System] Уведомление клиенту: платёж отклонён. Повторите оплату.");
            return;
        }
        hall.confirmBooking(req.date);
        System.out.println("[System] Бронирование подтверждено. Уведомление отправлено клиенту и администратору площадки.");
        List<String> tasks = List.of("Декорации", "Еда", "Оборудование");
        admin.prepareTasks(req, tasks);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Contractor> contractors = List.of(c1, c2, c3);
        CountDownLatch latch = new CountDownLatch(contractors.size());
        for (Contractor ct : contractors) {
            executor.submit(() -> {
                for (String task : tasks) {
                    ct.notifyTask(task);
                }
                for (String task : tasks) {
                    ct.confirmTask(task);
                }
                latch.countDown();
            });
        }
        latch.await();
        System.out.println("[System] Отчёт о статусе задач отправлен администратору площадки.");
        System.out.println("[System] Мероприятие проведено. Отправляем запрос клиента на оценку.");
        int rating = 5;
        System.out.println("[Client] Оценка: " + rating + " звёзд.");
        System.out.println("[System] Отчёт менеджеру отправлен.");
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("[System] Процесс бронирования и организации завершён.");
    }
}

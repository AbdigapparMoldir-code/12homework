import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
class Vacancy {
    enum Status { DRAFT, APPROVED, REWORK }
    String title;
    String description;
    Status status = Status.DRAFT;
    Vacancy(String title, String desc) { this.title = title; this.description = desc; }
}
class Candidate {
    static AtomicInteger idGen = new AtomicInteger(1);
    int id;
    String name;
    Map<String,String> resume;
    Candidate(String name, Map<String,String> resume) {
        this.id = idGen.getAndIncrement();
        this.name = name; this.resume = resume;
    }
}
class SystemDB {
    List<String> employees = new ArrayList<>();
    synchronized void addEmployee(String name) {
        employees.add(name);
        System.out.println("[SystemDB] Қосылды: " + name);
    }
}
class ITDepartment {
    void setupWorkspace(String employeeName) {
        System.out.println("[IT] Жұмыс орны дайындалуда: " + employeeName);
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        System.out.println("[IT] Жұмыс орны дайын: " + employeeName);
    }
}
class HR {
    boolean validateVacancy(Vacancy v) {
        boolean ok = v.title != null && !v.title.isBlank() && v.description != null && v.description.length() > 10;
        System.out.println("[HR] Вакансия тексерілді: " + (ok ? "айқындалды" : "қайтадан жазу керек"));
        return ok;
    }
    boolean evaluateCandidate(Candidate c) {
        String exp = c.resume.getOrDefault("experience", "0");
        try {
            int y = Integer.parseInt(exp);
            boolean ok = y >= 2;
            System.out.println("[HR] Кандидат " + c.name + " - " + (ok ? "сай келеді" : "сай емес"));
            return ok;
        } catch (NumberFormatException e) {
            System.out.println("[HR] Кандидат " + c.name + " - тәжірибе белгісіз, сай емес");
            return false;
        }
    }
    void inviteToInterview(Candidate c) {
        System.out.println("[HR] Шақырылуда (алғашқы): " + c.name);
    }
    void notifyRejection(Candidate c) {
        System.out.println("[HR] Қабылданбады: " + c.name);
    }
}
class Manager {
    String name;
    Manager(String name) { this.name = name; }
    Vacancy createVacancy(String title, String desc) {
        Vacancy v = new Vacancy(title, desc);
        System.out.println("[" + name + "] Вакансия жасалды: " + title);
        return v;
    }
    void technicalInterview(Candidate c, Consumer<Boolean> callback) {
        System.out.println("[" + name + "] Тех. интервью басталды: " + c.name);
        boolean pass = Integer.parseInt(c.resume.getOrDefault("skillsScore", "5")) >= 6;
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        System.out.println("[" + name + "] Тех. интервью аяқталды: " + c.name + " -> " + (pass ? "өту" : "сәтсіз"));
        callback.accept(pass);
    }
}
public class HiringProcess {
    public static void main(String[] args) throws InterruptedException {
        SystemDB db = new SystemDB();
        ITDepartment it = new ITDepartment();
        HR hr = new HR();
        Manager manager = new Manager("Руководитель отдела");
        Vacancy vac = manager.createVacancy("Java Developer", "Senior Java developer with microservices experience.");
        boolean vacOk = hr.validateVacancy(vac);
        if (!vacOk) {
            vac.status = Vacancy.Status.REWORK;
            System.out.println("[Система] Уведомление менеджеру: доработать вакансию.");
            return;
        } else {
            vac.status = Vacancy.Status.APPROVED;
            System.out.println("[Система] Вакансия утверждена HR.");
        }
        ExecutorService executor = Executors.newCachedThreadPool();
        System.out.println("[Система] Вакансия опубликована на сайте.");
        List<Candidate> applicants = Collections.synchronizedList(new ArrayList<>());
        Map<String,String> r1 = Map.of("experience","3","skillsScore","7");
        Map<String,String> r2 = Map.of("experience","1","skillsScore","5");
        Map<String,String> r3 = Map.of("experience","4","skillsScore","6");
        applicants.add(new Candidate("Aida", r1));
        applicants.add(new Candidate("Baur", r2));
        applicants.add(new Candidate("Saule", r3));
        List<Candidate> invited = Collections.synchronizedList(new ArrayList<>());
        List<Candidate> rejected = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(applicants.size());
        for (Candidate c : applicants) {
            executor.submit(() -> {
                boolean ok = hr.evaluateCandidate(c);
                if (ok) {
                    hr.inviteToInterview(c);
                    invited.add(c);
                } else {
                    hr.notifyRejection(c);
                    rejected.add(c);
                }
                latch.countDown();
            });
        }
        latch.await();
        for (Candidate c : invited) {
            System.out.println("[HR] Первичное интервью: " + c.name);
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}

            CountDownLatch techLatch = new CountDownLatch(1);
            final boolean[] techPass = {false};
            manager.technicalInterview(c, pass -> {
                techPass[0] = pass;
                techLatch.countDown();
            });
            techLatch.await();

            if (techPass[0]) {
                System.out.println("[Система] Оффер предлагется кандидату: " + c.name);
                boolean accept = new Random().nextBoolean();
                if (accept) {
                    System.out.println("[Candidate] " + c.name + " принял оффер.");
                    db.addEmployee(c.name);
                    executor.submit(() -> it.setupWorkspace(c.name));
                } else {
                    System.out.println("[Candidate] " + c.name + " отказался от оффера.");
                }
            } else {
                System.out.println("[Система] Кандидат " + c.name + " получил отказ после интервью.");
            }
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("[Система] Процесс найма завершён.");
        System.out.println("[SystemDB] Сотрудники: " + db.employees);
    }
}

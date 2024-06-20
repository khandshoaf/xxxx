import java.awt.*;
        import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalculatorUI extends Frame implements ActionListener {
    private static ArrayList<String> history = new ArrayList<>();
    private static boolean startNewCalculation = true;
    private static TextArea resultDisplay;
    private static TextArea historyDisplay;
    private static boolean isRunning = false;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private static final Object lock = new Object();

    public CalculatorUI() {
        setTitle("Simple Calculator");
        setLayout(new BorderLayout());

        Panel displayPanel = new Panel(new GridLayout(1, 2));

        // Tạo TextArea để hiển thị kết quả
        resultDisplay = new TextArea();
        resultDisplay.setEditable(false);

        // Tạo TextArea để hiển thị lịch sử
        historyDisplay = new TextArea();
        historyDisplay.setEditable(false);

        displayPanel.add(resultDisplay);
        displayPanel.add(historyDisplay);

        resultDisplay.setColumns(20);
        historyDisplay.setColumns(20);

        resultDisplay.setPreferredSize(new Dimension(100, 100));
        historyDisplay.setPreferredSize(new Dimension(100, 100));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.add(displayPanel);

        add(scrollPane, BorderLayout.NORTH);

        Panel buttonPanel = new Panel(new GridLayout(5, 5));

        String[] buttonLabels = {
                "C", "X", "Del", "/",
                "7", "8", "9", "*",
                "4", "5", "6", "-",
                "1", "2", "3", "+",
                "Hs", "0", ".", "="
        };

        for (String label : buttonLabels) {
            Button button = new Button(label);
            button.addActionListener(this);
            buttonPanel.add(button);
        }
        add(buttonPanel, BorderLayout.CENTER);
        setSize(300, 400);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if ("C".equals(command)) {
            resultDisplay.setText("");
        } else if ("=".equals(command)) {
            String input = resultDisplay.getText();
            if (!isRunning) {
                isRunning = true;

                executorService.submit(() -> {
                    try {
                        double result = calculateResult(input);
                        synchronized (lock) {
                            resultDisplay.setText(String.valueOf(result));
                            lock.notifyAll();
                        }
                    } catch (Exception ex) {
                        synchronized (lock) {
                            resultDisplay.setText("Error");
                            lock.notifyAll();
                        }
                    } finally {
                        isRunning = false;
                    }
                });

                executorService.submit(() -> {
                    synchronized (lock) {
                        while (isRunning) {
                            try {
                                lock.wait();
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                        displayHistory();
                    }
                });

                // Xóa phép tính cũ khi kết thúc phép tính
                startNewCalculation = true;
            }
        } else {
            if (startNewCalculation) {
                resultDisplay.setText(command);
                startNewCalculation = false;
            } else {
                resultDisplay.setText(resultDisplay.getText() + command);
            }
        }
        if ("Hs".equals(command)) {
            resultDisplay.setText("");
            executorService.submit(() -> {
                synchronized (lock) {
                    displayHistory();
                    startNewCalculation = true;
                    lock.notifyAll();
                }
            });
        } else if ("Del".equals(command)) {
            resultDisplay.setText("");
            executorService.submit(() -> {
                synchronized (lock) {
                    clearHistory();
                    startNewCalculation = true;
                    lock.notifyAll();
                }
            });
        }
        if ("X".equals(command)) {
            // Thoát ứng dụng khi click vào button "X"
            dispose();
            System.exit(0);
        }
    }


    private static  double calculateResult(String input) {
        String[] tokens = input.split("(?<=[-+*/])|(?=[-+*/])");
        double result = 0.0;
        double operand = 0.0;
        String operator = "+";

        for (String token : tokens) {
            if (isNumeric(token)) {
                operand = Double.parseDouble(token);
                switch (operator) {
                    case "+":
                        result += operand;
                        break;
                    case "-":
                        result -= operand;
                        break;
                    case "*":
                        result *= operand;
                        break;
                    case "/":
                        if (operand != 0) {
                            result /= operand;
                        } else {
                            resultDisplay.setText("Error");
                            System.out.println("Lỗi không chia cho 0!");
                            return Double.parseDouble("");
                        }
                        break;
                }
            } else {
                operator = token;
            }
        }

        System.out.println("Kết quả: " + input + " = " + result);
        resultDisplay.setText(input + " " + result);
        String resultString = Double.toString(result);
        history.add(input + " = " + resultString);

        isRunning = false;
        synchronized (lock) {
            lock.notifyAll(); // thông báo đến đối tượng khác đang chờ lock
        }
        return result;
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str); // chuyển đổi chuỗi str thành một số kiểu double
            return true; // nếu chuyển được trả về true
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void displayHistory() {
        System.out.println("Lịch sử phép tính");
        StringBuilder historyString = new StringBuilder();
        if (history.isEmpty()) {
            historyDisplay.setText("EMPTY");
            System.out.println("EMPTY");
        } else {
            for (String entry : history) {
                System.out.println(entry);
                historyString.append(entry).append("\n");
            }
            historyDisplay.setText(historyString.toString());
        }
    }

    private static void clearHistory() {
        if (history.isEmpty()) {
            historyDisplay.setText("NO DATA");
            System.out.println("không có dữ liệu");
        } else {
            history.clear();
            historyDisplay.setText("delete success");
            System.out.println("Xóa dữ liệu thành công");
        }
    }

    public static void main(String[] args) {
        new CalculatorUI();
    }
}
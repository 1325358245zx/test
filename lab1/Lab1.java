import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Lab1 {
    private static DirectedGraph graph = null;
    private static final double DAMPING_FACTOR = 0.85;
    private static JFrame frame;
    private static JTextArea outputArea;
    private static JTextField inputField;
    private static JButton fileButton, bridgeButton, textButton, pathButton, prButton, walkButton, visualizeButton;
    private static final Random random = new Random();
    
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--gui")) {
            createAndShowGUI();
        } else {
            runCommandLine(args);
        }
    }
    
    private static void runCommandLine(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String filePath = "";
        
        if (args.length > 0) {
            filePath = args[0];
        } else {
            System.out.println("请输入文本文件路径：");
            filePath = scanner.nextLine();
        }
        
        try {
            graph = new DirectedGraph(filePath);
            System.out.println("成功读取文件并生成有向图。");
            showDirectedGraph(graph);
            
            boolean running = true;
            while (running) {
                System.out.println("\n请选择功能：");
                System.out.println("1. 查询桥接词");
                System.out.println("2. 根据桥接词生成新文本");
                System.out.println("3. 计算最短路径");
                System.out.println("4. 计算PageRank值");
                System.out.println("5. 随机游走");
                System.out.println("6. 重新展示图");
                System.out.println("0. 退出程序");
                
                int choice = scanner.nextInt();
                scanner.nextLine(); // 清除换行符
                
                switch (choice) {
                    case 0:
                        running = false;
                        break;
                    case 1:
                        System.out.println("请输入第一个单词：");
                        String word1 = scanner.nextLine().toLowerCase();
                        System.out.println("请输入第二个单词：");
                        String word2 = scanner.nextLine().toLowerCase();
                        System.out.println(queryBridgeWords(word1, word2));
                        break;
                    case 2:
                        System.out.println("请输入文本：");
                        String inputText = scanner.nextLine();
                        System.out.println("生成的新文本：");
                        System.out.println(generateNewText(inputText));
                        break;
                    case 3:
                        System.out.println("请输入起始单词：");
                        String start = scanner.nextLine().toLowerCase();
                        System.out.println("请输入目标单词（若为空则计算到所有单词的最短路径）：");
                        String end = scanner.nextLine().toLowerCase();
                        if (end.isEmpty()) {
                            calculateAllShortestPaths(start);
                        } else {
                            System.out.println(calcShortestPath(start, end));
                        }
                        break;
                    case 4:
                        System.out.println("请输入要计算PageRank的单词（若为空则计算所有单词）：");
                        String prWord = scanner.nextLine().toLowerCase();
                        if (prWord.isEmpty()) {
                            calculateAllPageRanks();
                        } else {
                            double pr = calPageRank(prWord);
                            System.out.println("单词 '" + prWord + "' 的PageRank值为: " + pr);
                        }
                        break;
                    case 5:
                        System.out.println("随机游走结果：");
                        String walkResult = randomWalk();
                        System.out.println(walkResult);
                        break;
                    case 6:
                        showDirectedGraph(graph);
                        break;
                    default:
                        System.out.println("无效选择，请重试。");
                }
            }
        } catch (IOException e) {
            System.out.println("读取文件时发生错误: " + e.getMessage());
        }
        
        scanner.close();
    }
    
    private static void createAndShowGUI() {
        frame = new JFrame("文本图分析工具");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 输出区域
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 输入区域
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputPanel.add(inputField, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(1, 7));
        
        fileButton = new JButton("选择文件");
        fileButton.addActionListener(e -> loadFileGUI());
        
        bridgeButton = new JButton("桥接词");
        bridgeButton.addActionListener(e -> queryBridgeWordsGUI());
        bridgeButton.setEnabled(false);
        
        textButton = new JButton("生成文本");
        textButton.addActionListener(e -> generateNewTextGUI());
        textButton.setEnabled(false);
        
        pathButton = new JButton("最短路径");
        pathButton.addActionListener(e -> calcShortestPathGUI());
        pathButton.setEnabled(false);
        
        prButton = new JButton("PageRank");
        prButton.addActionListener(e -> calculatePageRankGUI());
        prButton.setEnabled(false);
        
        walkButton = new JButton("随机游走");
        walkButton.addActionListener(e -> randomWalkGUI());
        walkButton.setEnabled(false);
        
        visualizeButton = new JButton("可视化图");
        visualizeButton.addActionListener(e -> visualizeGraphGUI());
        visualizeButton.setEnabled(false);
        
        buttonPanel.add(fileButton);
        buttonPanel.add(bridgeButton);
        buttonPanel.add(textButton);
        buttonPanel.add(pathButton);
        buttonPanel.add(prButton);
        buttonPanel.add(walkButton);
        buttonPanel.add(visualizeButton);
        
        inputPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        
        frame.add(mainPanel);
        frame.setVisible(true);
        
        appendToOutput("欢迎使用文本图分析工具！\n请先选择一个文本文件以构建有向图。");
    }
    
    private static void loadFileGUI() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择文本文件");
        fileChooser.setFileFilter(new FileNameExtensionFilter("文本文件", "txt"));
        
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                graph = new DirectedGraph(selectedFile.getAbsolutePath());
                appendToOutput("成功读取文件并生成有向图。");
                
                // 启用所有功能按钮
                bridgeButton.setEnabled(true);
                textButton.setEnabled(true);
                pathButton.setEnabled(true);
                prButton.setEnabled(true);
                walkButton.setEnabled(true);
                visualizeButton.setEnabled(true);
                
                // 显示图结构
                String graphStr = getGraphString(graph);
                appendToOutput("\n图结构:\n" + graphStr);
            } catch (IOException e) {
                appendToOutput("读取文件时发生错误: " + e.getMessage());
            }
        }
    }
    
    private static void queryBridgeWordsGUI() {
        String input = JOptionPane.showInputDialog(frame, "请输入两个单词，用空格分隔：");
        if (input != null && !input.trim().isEmpty()) {
            String[] words = input.trim().split("\\s+");
            if (words.length == 2) {
                String result = queryBridgeWords(words[0].toLowerCase(), words[1].toLowerCase());
                appendToOutput("\n" + result);
            } else {
                appendToOutput("\n请输入恰好两个单词！");
            }
        }
    }
    
    private static void generateNewTextGUI() {
        String input = inputField.getText().trim();
        if (!input.isEmpty()) {
            String result = generateNewText(input);
            appendToOutput("\n原文本: " + input);
            appendToOutput("\n生成的新文本: " + result);
            inputField.setText("");
        } else {
            appendToOutput("\n请在输入框中输入文本！");
        }
    }
    
    private static void calcShortestPathGUI() {
        String input = JOptionPane.showInputDialog(frame, "请输入起始单词和目标单词，用空格分隔（如果只输入一个单词，将计算到所有单词的最短路径）：");
        if (input != null && !input.trim().isEmpty()) {
            String[] words = input.trim().split("\\s+");
            if (words.length == 1) {
                calculateAllShortestPathsGUI(words[0].toLowerCase());
            } else if (words.length >= 2) {
                String result = calcShortestPath(words[0].toLowerCase(), words[1].toLowerCase());
                appendToOutput("\n" + result);
            }
        }
    }
    
    private static void calculateAllShortestPathsGUI(String startWord) {
        appendToOutput("\n从 '" + startWord + "' 到所有单词的最短路径：");
        
        if (!graph.containsWord(startWord)) {
            appendToOutput("\n图中不存在单词 '" + startWord + "'！");
            return;
        }
        
        Map<String, List<String>> paths = new HashMap<>();
        Map<String, Double> distances = new HashMap<>();
        
        calculateShortestPaths(startWord, paths, distances);
        
        List<Map.Entry<String, Double>> sortedDistances = new ArrayList<>(distances.entrySet());
        sortedDistances.sort(Map.Entry.comparingByValue());
        
        for (Map.Entry<String, Double> entry : sortedDistances) {
            String endWord = entry.getKey();
            double distance = entry.getValue();
            List<String> path = paths.get(endWord);
            
            if (distance != Double.POSITIVE_INFINITY && !endWord.equals(startWord)) {
                StringBuilder pathStr = new StringBuilder();
                for (String word : path) {
                    pathStr.append(word).append(" → ");
                }
                pathStr.delete(pathStr.length() - 3, pathStr.length());
                
                appendToOutput("\n到 '" + endWord + "' 的最短路径: " + pathStr.toString() + ", 长度: " + distance);
            }
        }
    }
    
    private static void calculatePageRankGUI() {
        String input = JOptionPane.showInputDialog(frame, "请输入要计算PageRank的单词（留空计算所有单词）：");
        if (input != null) {
            if (input.trim().isEmpty()) {
                calculateAllPageRanksGUI();
            } else {
                String word = input.trim().toLowerCase();
                double pr = calPageRank(word);
                appendToOutput("\n单词 '" + word + "' 的PageRank值为: " + pr);
            }
        }
    }
    
    private static void calculateAllPageRanksGUI() {
        appendToOutput("\n所有单词的PageRank值（按降序排列）：");
        Map<String, Double> pageRanks = calculateAllPageRanksMap();
        
        // 按PageRank值降序排序
        List<Map.Entry<String, Double>> sortedRanks = new ArrayList<>(pageRanks.entrySet());
        sortedRanks.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
        
        for (Map.Entry<String, Double> entry : sortedRanks) {
            appendToOutput("\n'" + entry.getKey() + "': " + entry.getValue());
        }
    }
    
    private static void randomWalkGUI() {
        String result = randomWalk();
        appendToOutput("\n随机游走结果：\n" + result);
    }
    
    private static void visualizeGraphGUI() {
        appendToOutput("\n生成图可视化图像...");
        
        try {
            int width = 800;
            int height = 600;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            
            // 设置白色背景
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            
            // 绘制图
            visualizeGraph(g, width, height);
            
            g.dispose();
            
            // 保存图像
            File outputFile = new File("graph_visualization.png");
            ImageIO.write(image, "png", outputFile);
            
            appendToOutput("\n图可视化已保存到: " + outputFile.getAbsolutePath());
            
            // 弹出窗口显示图像
            JFrame imageFrame = new JFrame("图可视化");
            JLabel imageLabel = new JLabel(new ImageIcon(image));
            JScrollPane scrollPane = new JScrollPane(imageLabel);
            imageFrame.add(scrollPane);
            imageFrame.setSize(850, 650);
            imageFrame.setVisible(true);
            
        } catch (Exception e) {
            appendToOutput("\n生成可视化图时出错: " + e.getMessage());
        }
    }
    
    private static void visualizeGraph(Graphics2D g, int width, int height) {
        Set<String> words = graph.getAllWords();
        int nodeCount = words.size();
        
        if (nodeCount == 0) return;
        
        // 节点位置映射
        Map<String, Point> nodePositions = new HashMap<>();
        
        // 计算节点位置（圆形布局）
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 3;
        
        int i = 0;
        for (String word : words) {
            double angle = 2 * Math.PI * i / nodeCount;
            int x = centerX + (int)(radius * Math.cos(angle));
            int y = centerY + (int)(radius * Math.sin(angle));
            nodePositions.put(word, new Point(x, y));
            i++;
        }
        
        // 绘制边
        g.setColor(Color.GRAY);
        Map<String, Map<String, Integer>> edgeWeights = graph.getEdgeWeights();
        
        for (String fromWord : edgeWeights.keySet()) {
            Point fromPoint = nodePositions.get(fromWord);
            
            for (Map.Entry<String, Integer> edge : edgeWeights.get(fromWord).entrySet()) {
                String toWord = edge.getKey();
                int weight = edge.getValue();
                Point toPoint = nodePositions.get(toWord);
                
                // 计算箭头
                drawArrow(g, fromPoint.x, fromPoint.y, toPoint.x, toPoint.y);
                
                // 绘制权重
                int labelX = (fromPoint.x + toPoint.x) / 2;
                int labelY = (fromPoint.y + toPoint.y) / 2;
                g.drawString(String.valueOf(weight), labelX, labelY);
            }
        }
        
        // 绘制节点
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        for (Map.Entry<String, Point> entry : nodePositions.entrySet()) {
            String word = entry.getKey();
            Point point = entry.getValue();
            
            // 绘制节点圆圈
            g.setColor(Color.LIGHT_GRAY);
            int nodeSize = 30;
            g.fillOval(point.x - nodeSize/2, point.y - nodeSize/2, nodeSize, nodeSize);
            
            // 绘制节点文本
            g.setColor(Color.BLACK);
            int textWidth = g.getFontMetrics().stringWidth(word);
            g.drawString(word, point.x - textWidth/2, point.y + 5);
        }
    }
    
    private static void drawArrow(Graphics2D g, int x1, int y1, int x2, int y2) {
        int nodeRadius = 15; // 节点半径
        
        // 计算实际绘制线的起点和终点（考虑节点大小）
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int startX = (int)(x1 + nodeRadius * Math.cos(angle));
        int startY = (int)(y1 + nodeRadius * Math.sin(angle));
        int endX = (int)(x2 - nodeRadius * Math.cos(angle));
        int endY = (int)(y2 - nodeRadius * Math.sin(angle));
        
        g.drawLine(startX, startY, endX, endY);
        
        // 绘制箭头
        int arrowSize = 7;
        int dx = (int)(arrowSize * Math.cos(angle - Math.PI/6));
        int dy = (int)(arrowSize * Math.sin(angle - Math.PI/6));
        g.drawLine(endX, endY, endX - dx, endY - dy);
        
        dx = (int)(arrowSize * Math.cos(angle + Math.PI/6));
        dy = (int)(arrowSize * Math.sin(angle + Math.PI/6));
        g.drawLine(endX, endY, endX - dx, endY - dy);
    }
    
    private static void appendToOutput(String text) {
        outputArea.append(text + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }
    
    // 实现所需的核心函数
    public static void showDirectedGraph(DirectedGraph G) {
        System.out.println("有向图结构：");
        System.out.println(getGraphString(G));
    }
    
    private static String getGraphString(DirectedGraph G) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("节点数: ").append(G.getAllWords().size()).append("\n");
        sb.append("边数: ").append(G.getEdgeCount()).append("\n\n");
        
        sb.append("边列表（格式：起点 -> 终点 [权重]）：\n");
        Map<String, Map<String, Integer>> edgeWeights = G.getEdgeWeights();
        
        for (String fromWord : edgeWeights.keySet()) {
            for (Map.Entry<String, Integer> edge : edgeWeights.get(fromWord).entrySet()) {
                String toWord = edge.getKey();
                int weight = edge.getValue();
                sb.append(fromWord).append(" -> ").append(toWord).append(" [").append(weight).append("]\n");
            }
        }
        
        return sb.toString();
    }
    
    public static String queryBridgeWords(String word1, String word2) {
        if (graph == null) {
            return "请先加载文本文件生成图！";
        }
        
        // 检查两个单词是否在图中
        if (!graph.containsWord(word1) || !graph.containsWord(word2)) {
            return "No " + ((!graph.containsWord(word1)) ? "word1" : "word2") + " or " + 
                   ((!graph.containsWord(word2)) ? "word2" : "word1") + " in the graph!";
        }
        
        // 查找桥接词
        List<String> bridges = new ArrayList<>();
        Map<String, Integer> outEdges = graph.getOutEdges(word1);
        
        if (outEdges != null) {
            for (String mid : outEdges.keySet()) {
                Map<String, Integer> midOutEdges = graph.getOutEdges(mid);
                if (midOutEdges != null && midOutEdges.containsKey(word2)) {
                    bridges.add(mid);
                }
            }
        }
        
        // 根据桥接词数量返回适当的消息
        if (bridges.isEmpty()) {
            return "No bridge words from " + word1 + " to " + word2 + "!";
        } else {
            StringBuilder result = new StringBuilder("The bridge words from " + word1 + " to " + word2 + " are: ");
            
            for (int i = 0; i < bridges.size(); i++) {
                if (i > 0) {
                    if (i == bridges.size() - 1) {
                        result.append(" and ");
                    } else {
                        result.append(", ");
                    }
                }
                result.append(bridges.get(i));
            }
            result.append(".");
            
            return result.toString();
        }
    }
    
    public static String generateNewText(String inputText) {
        if (graph == null) {
            return "请先加载文本文件生成图！";
        }
        
        // 处理输入文本，分解为单词
        String[] words = inputText.toLowerCase().replaceAll("[^a-z\\s]", " ").split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            result.append(words[i]);
            
            // 如果不是最后一个单词，则尝试插入桥接词
            if (i < words.length - 1) {
                String currentWord = words[i];
                String nextWord = words[i + 1];
                
                if (graph.containsWord(currentWord) && graph.containsWord(nextWord)) {
                    // 查找桥接词
                    List<String> bridges = new ArrayList<>();
                    Map<String, Integer> outEdges = graph.getOutEdges(currentWord);
                    
                    if (outEdges != null) {
                        for (String mid : outEdges.keySet()) {
                            Map<String, Integer> midOutEdges = graph.getOutEdges(mid);
                            if (midOutEdges != null && midOutEdges.containsKey(nextWord)) {
                                bridges.add(mid);
                            }
                        }
                    }
                    
                    // 如果有桥接词，随机选择一个插入
                    if (!bridges.isEmpty()) {
                        String bridge = bridges.get(random.nextInt(bridges.size()));
                        result.append(" ").append(bridge);
                    }
                }
                
                result.append(" ");
            }
        }
        
        return result.toString();
    }
    
    public static String calcShortestPath(String word1, String word2) {
        if (graph == null) {
            return "请先加载文本文件生成图！";
        }
        
        // 检查单词是否在图中
        if (!graph.containsWord(word1)) {
            return "图中不存在单词 '" + word1 + "'！";
        }
        
        if (!graph.containsWord(word2)) {
            return "图中不存在单词 '" + word2 + "'！";
        }
        
        // 使用Dijkstra算法计算最短路径
        Map<String, List<String>> paths = new HashMap<>();
        Map<String, Double> distances = new HashMap<>();
        
        calculateShortestPaths(word1, paths, distances);
        
        // 检查是否存在路径
        if (distances.get(word2) == Double.POSITIVE_INFINITY) {
            return "从 '" + word1 + "' 到 '" + word2 + "' 不存在路径！";
        }
        
        // 构建路径字符串
        List<String> path = paths.get(word2);
        StringBuilder pathStr = new StringBuilder();
        
        for (int i = 0; i < path.size(); i++) {
            pathStr.append(path.get(i));
            if (i < path.size() - 1) {
                pathStr.append(" → ");
            }
        }
        
        return "从 '" + word1 + "' 到 '" + word2 + "' 的最短路径是: " + 
               pathStr.toString() + ", 长度: " + distances.get(word2);
    }
    
    private static void calculateShortestPaths(String startWord, Map<String, List<String>> paths, Map<String, Double> distances) {
        Set<String> allWords = graph.getAllWords();
        Set<String> unvisited = new HashSet<>(allWords);
        
        // 初始化距离
        for (String word : allWords) {
            distances.put(word, Double.POSITIVE_INFINITY);
            paths.put(word, new ArrayList<>());
        }
        
        distances.put(startWord, 0.0);
        List<String> startPath = new ArrayList<>();
        startPath.add(startWord);
        paths.put(startWord, startPath);
        
        while (!unvisited.isEmpty()) {
            // 找出距离最小的未访问节点
            String current = null;
            double minDistance = Double.POSITIVE_INFINITY;
            
            for (String word : unvisited) {
                double dist = distances.get(word);
                if (dist < minDistance) {
                    minDistance = dist;
                    current = word;
                }
            }
            
            // 如果没有找到可访问的节点，结束循环
            if (current == null || distances.get(current) == Double.POSITIVE_INFINITY) {
                break;
            }
            
            // 从未访问集合中移除当前节点
            unvisited.remove(current);
            
            // 更新邻居的距离
            Map<String, Integer> neighbors = graph.getOutEdges(current);
            if (neighbors != null) {
                for (Map.Entry<String, Integer> edge : neighbors.entrySet()) {
                    String neighbor = edge.getKey();
                    double weight = edge.getValue();
                    double distanceThroughCurrent = distances.get(current) + weight;
                    
                    if (distanceThroughCurrent < distances.get(neighbor)) {
                        distances.put(neighbor, distanceThroughCurrent);
                        
                        // 更新路径
                        List<String> newPath = new ArrayList<>(paths.get(current));
                        newPath.add(neighbor);
                        paths.put(neighbor, newPath);
                    }
                }
            }
        }
    }
    
    private static void calculateAllShortestPaths(String startWord) {
        if (!graph.containsWord(startWord)) {
            System.out.println("图中不存在单词 '" + startWord + "'！");
            return;
        }
        
        Map<String, List<String>> paths = new HashMap<>();
        Map<String, Double> distances = new HashMap<>();
        
        calculateShortestPaths(startWord, paths, distances);
        
        System.out.println("从 '" + startWord + "' 到所有单词的最短路径：");
        
        List<Map.Entry<String, Double>> sortedDistances = new ArrayList<>(distances.entrySet());
        sortedDistances.sort(Map.Entry.comparingByValue());
        
        for (Map.Entry<String, Double> entry : sortedDistances) {
            String endWord = entry.getKey();
            double distance = entry.getValue();
            List<String> path = paths.get(endWord);
            
            if (distance != Double.POSITIVE_INFINITY && !endWord.equals(startWord)) {
                System.out.print("到 '" + endWord + "' 的最短路径: ");
                
                for (int i = 0; i < path.size(); i++) {
                    System.out.print(path.get(i));
                    if (i < path.size() - 1) {
                        System.out.print(" → ");
                    }
                }
                
                System.out.println(", 长度: " + distance);
            }
        }
    }
    
    public static double calPageRank(String word) {
        if (graph == null) {
            return 0.0;
        }
        
        if (!graph.containsWord(word)) {
            return 0.0;
        }
        
        // 计算所有单词的PageRank
        Map<String, Double> pageRanks = calculateAllPageRanksMap();
        
        return pageRanks.getOrDefault(word, 0.0);
    }
    
    private static Map<String, Double> calculateAllPageRanksMap() {
        Set<String> allWords = graph.getAllWords();
        int n = allWords.size();
        
        if (n == 0) {
            return new HashMap<>();
        }
        
        // 初始化PageRank值
        Map<String, Double> pageRanks = new HashMap<>();
        for (String word : allWords) {
            pageRanks.put(word, 1.0 / n);
        }
        
        // 迭代计算PageRank（通常20-50次迭代足够）
        int iterations = 30;
        for (int iter = 0; iter < iterations; iter++) {
            Map<String, Double> newPageRanks = new HashMap<>();
            
            // 初始化新的PageRank值为(1-d)/n
            for (String word : allWords) {
                newPageRanks.put(word, (1 - DAMPING_FACTOR) / n);
            }
            
            // 计算出度为0的节点的贡献
            double sinkPR = 0.0;
            for (String word : allWords) {
                Map<String, Integer> outEdges = graph.getOutEdges(word);
                if (outEdges == null || outEdges.isEmpty()) {
                    sinkPR += pageRanks.get(word);
                }
            }
            
            // 将出度为0的节点的PR值均匀分配给所有节点
            for (String word : allWords) {
                newPageRanks.put(word, newPageRanks.get(word) + DAMPING_FACTOR * sinkPR / n);
            }
            
            // 根据链接关系更新PageRank
            for (String word : allWords) {
                Map<String, Integer> outEdges = graph.getOutEdges(word);
                if (outEdges != null && !outEdges.isEmpty()) {
                    double outDegree = 0.0;
                    for (int weight : outEdges.values()) {
                        outDegree += weight;
                    }
                    
                    for (Map.Entry<String, Integer> edge : outEdges.entrySet()) {
                        String toWord = edge.getKey();
                        double weight = edge.getValue();
                        
                        // 考虑权重的贡献
                        double contribution = DAMPING_FACTOR * pageRanks.get(word) * (weight / outDegree);
                        newPageRanks.put(toWord, newPageRanks.get(toWord) + contribution);
                    }
                }
            }
            
            // 更新PageRank值
            pageRanks = newPageRanks;
        }
        
        return pageRanks;
    }
    
    private static void calculateAllPageRanks() {
        Map<String, Double> pageRanks = calculateAllPageRanksMap();
        
        // 按PageRank值降序排序
        List<Map.Entry<String, Double>> sortedRanks = new ArrayList<>(pageRanks.entrySet());
        sortedRanks.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
        
        System.out.println("所有单词的PageRank值（按降序排列）：");
        for (Map.Entry<String, Double> entry : sortedRanks) {
            System.out.println("'" + entry.getKey() + "': " + entry.getValue());
        }
    }
    
    public static String randomWalk() {
        if (graph == null) {
            return "请先加载文本文件生成图！";
        }
        
        Set<String> allWords = graph.getAllWords();
        if (allWords.isEmpty()) {
            return "图中没有节点！";
        }
        
        // 随机选择起始节点
        List<String> wordsList = new ArrayList<>(allWords);
        String currentWord = wordsList.get(random.nextInt(wordsList.size()));
        
        StringBuilder path = new StringBuilder(currentWord);
        Set<String> visitedEdges = new HashSet<>();
        
        while (true) {
            // 获取当前单词的出边
            Map<String, Integer> outEdges = graph.getOutEdges(currentWord);
            
            // 如果没有出边，结束游走
            if (outEdges == null || outEdges.isEmpty()) {
                path.append("\n[随机游走结束: 没有出边]");
                break;
            }
            
            // 将出边转换为列表以便随机选择
            List<Map.Entry<String, Integer>> edgesList = new ArrayList<>(outEdges.entrySet());
            
            // 随机选择一条边
            Map.Entry<String, Integer> selectedEdge = edgesList.get(random.nextInt(edgesList.size()));
            String nextWord = selectedEdge.getKey();
            
            // 检查边是否已被访问
            String edgeKey = currentWord + "->" + nextWord;
            if (visitedEdges.contains(edgeKey)) {
                path.append("\n[随机游走结束: 遇到重复边 " + edgeKey + "]");
                break;
            }
            
            // 添加边到已访问集合
            visitedEdges.add(edgeKey);
            
            // 更新当前单词并追加到路径
            currentWord = nextWord;
            path.append(" ").append(currentWord);
        }
        
        // 保存结果到文件
        String filename = "random_walk_result.txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println(path.toString());
            return path.toString() + "\n\n[结果已保存到文件: " + filename + "]";
        } catch (IOException e) {
            return path.toString() + "\n\n[保存到文件失败: " + e.getMessage() + "]";
        }
    }
    
    // 有向图类定义
    static class DirectedGraph {
        private Map<String, Map<String, Integer>> adjacencyList; // 邻接表
        
        public DirectedGraph(String filePath) throws IOException {
            adjacencyList = new HashMap<>();
            loadFromFile(filePath);
        }
        
        private void loadFromFile(String filePath) throws IOException {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append(" ");
                }
            }
            
            // 处理文本：转换为小写，替换标点符号为空格，忽略非字母字符
            String processedText = content.toString().toLowerCase();
            processedText = processedText.replaceAll("[^a-z]", " ");
            processedText = processedText.replaceAll("\\s+", " ").trim();
            
            // 分割成单词
            String[] words = processedText.split("\\s+");
            
            // 构建有向图
            for (int i = 0; i < words.length - 1; i++) {
                String fromWord = words[i];
                String toWord = words[i + 1];
                
                // 添加节点和边
                if (!adjacencyList.containsKey(fromWord)) {
                    adjacencyList.put(fromWord, new HashMap<>());
                }
                
                Map<String, Integer> outEdges = adjacencyList.get(fromWord);
                outEdges.put(toWord, outEdges.getOrDefault(toWord, 0) + 1);
            }
            
            // 确保最后一个单词也在图中（可能没有出边）
            if (words.length > 0) {
                String lastWord = words[words.length - 1];
                if (!adjacencyList.containsKey(lastWord)) {
                    adjacencyList.put(lastWord, new HashMap<>());
                }
            }
        }
        
        public boolean containsWord(String word) {
            return adjacencyList.containsKey(word);
        }
        
        public Set<String> getAllWords() {
            return adjacencyList.keySet();
        }
        
        public Map<String, Integer> getOutEdges(String word) {
            return adjacencyList.get(word);
        }
        
        public Map<String, Map<String, Integer>> getEdgeWeights() {
            return adjacencyList;
        }
        
        public int getEdgeCount() {
            int count = 0;
            for (Map<String, Integer> edges : adjacencyList.values()) {
                count += edges.size();
            }
            return count;
        }
    }
}
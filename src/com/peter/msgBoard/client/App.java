package com.peter.msgBoard.client;

import com.peter.msgBoard.utils.Utils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 控制应用的主逻辑
 */
public class App {
    // ********************************** Members ************************************* //

    protected static final String APP_VERSION = "1.14.1";

    // 帮助文档
    private static final String[] HELP_DOCS = {
            "使用应用前请手动连接至服务器",
            "当应用提示\"请重新连接服务器\"或类似字样时，表明您可能需要手动重新连接服务器，或服务器已关机",
            "在进行输入操作时，输入空白内容即可退出输入，从而回到命令输入模式",
            "在命令输入模式下，输入\"menu\"可以打印菜单，避免菜单被挤压到过上的位置",
            "程序会检测本地编码，请使用系统的默认编码操作本应用，否则可能出现乱码",
            "彩蛋：\"Furukawa Nagisa\"(不分大小写)，希望能给您一些娱乐",
            "在命令输入模式下，输入\"/help\"即可再次打开本说明文档"
    };

    // 获取系统编码格式
    private static final String SYSTEM_CHARSET = Utils.getSysCharset();

    // 客户端实例
    private final Client client;

    // 客户端操作实例
    private final ClientOperations operations;

    /**
     * 一级菜单
     * 1. 用户登录
     * 2. 查看所有用户
     * 3. 查看在线用户
     * 4. 连接到服务器
     * 5. 进入留言板
     * 6. 添加用户
     * 7. 删除用户
     * 0. 退出应用
     */
    private static final String[] mainOptionList = {"用户登录", "查看所有用户", "查看在线用户", "连接到服务器", "进入留言板", "添加用户", "删除用户", "退出应用"};

    /**
     * 二级菜单
     * 1. 查看历史留言
     * 2. 编写新的留言
     * 3. 退出留言板
     * 4. 退出登录
     * 0. 退出应用
     */
    private static final String[] secondaryOptionList = {"查看历史留言", "编写新的留言", "退出留言板", "退出应用"};

    // 设置输入解码格式为系统默认编码
    protected static final Scanner scanner = new Scanner(System.in, Charset.forName(SYSTEM_CHARSET));

    // 一级操作及其方法的映射表
    private final Map<String, Runnable> mainOptionMap;

    // 二级操作及其方法的映射表
    private final Map<String, Runnable> secondaryOptionMap;


    // ********************************** Functions ************************************* //

    /**
     * 构造函数
     * 初始化对象所需的资源，然后调用初始化方法
     */
    protected App(Client client) {
        this.client = client;
        this.operations = new ClientOperations(client, this);
        this.mainOptionMap = new HashMap<>();
        this.secondaryOptionMap = new HashMap<>();
        this.init();
    }

    /**
     * 初始化应用
     * 1. 将一级和二级菜单的操作与对应方法做映射，大部分操作方法在ClientOperations类被实现
     * 2. 输出操作系统的编码环境和欢迎语
     */
    private void init() {
        // 将数字操作命令与方法做映射
        // 一级菜单
        this.mainOptionMap.put("0", this::quitApp);
        this.mainOptionMap.put("1", this.operations::userLogin);
        this.mainOptionMap.put("2", this.operations::displayAllUsers);
        this.mainOptionMap.put("3", this.operations::displayOnlineUsers);
        this.mainOptionMap.put("4", this.operations::connect);
        this.mainOptionMap.put("5", this::enterMsgBoard);
        this.mainOptionMap.put("6", this.operations::addUser);
        this.mainOptionMap.put("7", this.operations::delUser);
        this.mainOptionMap.put("/help", this::showHelp);

        // 二级菜单
        this.secondaryOptionMap.put("0", this::quitApp);
        this.secondaryOptionMap.put("1", this.operations::showAllMessages);
        this.secondaryOptionMap.put("2", this.operations::writeNewMessage);
        this.secondaryOptionMap.put("/help", this::showHelp);

        // 输出欢迎语
        System.out.println("当前操作系统编码：" + SYSTEM_CHARSET);
        System.out.println();
        System.out.println("欢迎使用本留言板，请输入数字命令进行操作。");
    }

    /**
     * 启动应用主循环，进入一级菜单
     * 1. 循环获取用户输入，根据映射表找到输入操作对应的方法并执行。没找到则打印 “谜之操作...”
     */
    protected void start() {
        this.showMainMenu();
        while (true) {
            System.out.print("输入命令：");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("menu")) {
                this.printNewMainMenu();
            } else if (input.equalsIgnoreCase("Furukawa Nagisa")) {
                Utils.toggleEasterEgg();
                break;
            } else {
                this.switchOption(input, this.mainOptionMap);
            }
        }
    }

    // 打印一级菜单
    private void showMainMenu() {
        int listLen = mainOptionList.length;
        String curUserName = this.client.getCurUserName();
        System.out.println("当前登录用户：" + (curUserName == null ? "无" : curUserName));
        this._showMenu(listLen, mainOptionList);
    }

    // 打印二级菜单
    private void showSecondaryMenu() {
        int listLen = secondaryOptionList.length;
        String curUserName = this.client.getCurUserName();
        System.out.println("欢迎来到留言板，" + curUserName + "！");
        this._showMenu(listLen, secondaryOptionList);
    }

    private void _showMenu(int listLen, String[] optionList) {
        String serverAddr = this.client.getServerAddr();
        System.out.println("服务器地址：" + (serverAddr == null ? "暂未连接" : serverAddr));
        System.out.println("输入\"/help\"以打开说明文档");
        System.out.println("------------------------------------------");
        for (int i = 0; i < listLen; i++) {
            if (i == listLen - 1) {
                System.out.println(0 + ". " + optionList[i]);
                break;
            }
            System.out.println(i + 1 + ". " + optionList[i]);
        }
        System.out.println("------------------------------------------");
    }

    /**
     * 处理退出App的逻辑
     * 1. 当用户输入值为 y 时，退出程序，否则不做任何操作
     * 2. 退出程序前，主动断开与服务器的连接
     */
    private void quitApp() {
        System.out.print("是否要退出应用(y/n)：");
        String input = scanner.nextLine().trim();
        if ("y".equalsIgnoreCase(input)) {
            if (!this.client.isNotConnected())
                this.client.sendRequest(this.client.getSocket(), "quit", null, null);
            System.out.println("欢迎下次使用！");
            System.exit(0);
        }
        System.out.println("无事发生...");
    }

    // 打印三个空行
    protected void printBlank() {
        System.out.println();
        System.out.println();
        System.out.println();
    }

    // 打印三个空行，然后打印新的主菜单
    protected void printNewMainMenu() {
        this.printBlank();
        this.showMainMenu();
    }

    // 打印三个空行，然后打印新的二级菜单
    protected void printNewSecondaryMenu() {
        this.printBlank();
        this.showSecondaryMenu();
    }

    /**
     * 进入留言板
     */
    private void enterMsgBoard() {
        if (this.client.isNotConnected()) return;
        if (this.client.isNotLogin()) return;

        this.printNewSecondaryMenu();
        this.startSecondaryLoop();
    }

    /**
     * 开启二级循环，控制留言板内的逻辑
     */
    protected void startSecondaryLoop() {
        while (true) {
            System.out.print("输入命令：");
            String input = scanner.nextLine().trim();
            if (input.equals("3")) {
                this.printNewMainMenu();
                break;
            } else if (input.equals("menu")) {
                this.printNewSecondaryMenu();
            } else {
                this.switchOption(input, this.secondaryOptionMap);
            }
        }
    }

    private void switchOption(String option, Map<String, Runnable> map) {
        Runnable runnable = map.get(option);
        if (runnable == null) {
            System.out.println("迷之操作...");
            return;
        }
        runnable.run();
    }

    protected void showHelp() {
        System.out.println("------------------------------------------");
        System.out.println("感谢您使用本应用。");
        System.out.println("程序版本：" + APP_VERSION);
        System.out.println("帮助文档：");
        for (int i = 0; i < HELP_DOCS.length; i++) {
            System.out.println("  " + (i + 1) + ". " + HELP_DOCS[i] + "。");
        }
        System.out.println("------------------------------------------");
        System.out.print("输入任意内容以回到主页：");
        scanner.nextLine();
    }
}

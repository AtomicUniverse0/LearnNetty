package client;

import message.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.LinkedHashSet;
import java.util.Set;

import static util.ArgumentChecker.requireText;

/** 将空白分隔的一行命令转换为消息；send/gsend 的剩余部分作为正文。 */
public final class ChatCommandParser {
    private final String username;

    public ChatCommandParser(String username) {
        this.username = requireText(username, "当前用户名");
    }

    public Message parse(String input) {
        requireText(input, "命令");
        if (input.indexOf('\n') >= 0 || input.indexOf('\r') >= 0) {
            throw new CommandParseException("请输入单行命令");
        }
        // 每次建立新的命令对象，避免上一次解析的参数残留。
        CommandLine cli = new CommandLine(new Root());
        cli.addSubcommand("send", new Send());
        cli.addSubcommand("gsend", new GroupSend());
        cli.addSubcommand("gcreate", new GroupCreate());
        cli.addSubcommand("gmembers", new GroupMembers());
        cli.addSubcommand("gjoin", new GroupJoin());
        cli.addSubcommand("gquit", new GroupQuit());
        cli.addSubcommand("quit", new Quit());
        cli.setExpandAtFiles(false);
        for (CommandLine command : cli.getSubcommands().values()) {
            command.setExpandAtFiles(false);
            command.setStopAtPositional(true);
            command.setUnmatchedOptionsArePositionalParams(true);
        }
        try {
            CommandLine.ParseResult result = cli.parseArgs(input.strip().split("\\s+"));
            if (!result.hasSubcommand()) {
                throw new CommandParseException("缺少命令");
            }
            ParsedCommand command = (ParsedCommand) result.subcommand().commandSpec().userObject();
            return command.toMessage(username);
        } catch (CommandLine.ParameterException e) {
            throw new CommandParseException("命令格式错误: " + e.getMessage(), e);
        }
    }

    private interface ParsedCommand {
        Message toMessage(String username);
    }

    @Command(name = "chat")
    private static class Root { }

    private abstract static class TextCommand implements ParsedCommand {
        @Parameters(index = "0", arity = "1", paramLabel = "目标")
        String target;

        @Parameters(index = "1..*", arity = "1..*", paramLabel = "正文")
        String[] content;

        String text() {
            return requireText(String.join(" ", content), "正文");
        }
    }

    @Command(name = "send")
    private static class Send extends TextCommand {
        public Message toMessage(String username) {
            return new ChatRequestMessage(username, target, text());
        }
    }

    @Command(name = "gsend")
    private static class GroupSend extends TextCommand {
        public Message toMessage(String username) {
            return new GroupChatRequestMessage(username, target, text());
        }
    }

    @Command(name = "gcreate")
    private static class GroupCreate implements ParsedCommand {
        @Parameters(index = "0", arity = "1", paramLabel = "群名")
        String groupName;

        @Parameters(index = "1", arity = "0..1", paramLabel = "成员1,成员2,...")
        String members;

        public Message toMessage(String username) {
            Set<String> initialMembers = new LinkedHashSet<>();
            initialMembers.add(username);
            if (members != null) {
                for (String member : members.split(",", -1)) {
                    initialMembers.add(requireText(member, "成员名"));
                }
            }
            return new GroupCreateRequestMessage(groupName, initialMembers);
        }
    }

    private abstract static class GroupCommand implements ParsedCommand {
        @Parameters(index = "0", arity = "1", paramLabel = "群名")
        String groupName;
    }

    @Command(name = "gmembers")
    private static class GroupMembers extends GroupCommand {
        public Message toMessage(String username) {
            return new GroupMembersRequestMessage(groupName);
        }
    }

    @Command(name = "gjoin")
    private static class GroupJoin extends GroupCommand {
        public Message toMessage(String username) {
            return new GroupJoinRequestMessage(username, groupName);
        }
    }

    @Command(name = "gquit")
    private static class GroupQuit extends GroupCommand {
        public Message toMessage(String username) {
            return new GroupQuitRequestMessage(username, groupName);
        }
    }

    @Command(name = "quit")
    private static class Quit implements ParsedCommand {
        public Message toMessage(String username) {
            return new QuitRequestMessage();
        }
    }
}

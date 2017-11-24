package de.btobastian.sdcf4j.handler;

import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Getter(AccessLevel.PACKAGE)
class CommandSplitter {

    private final String command;
    private final List<String> args;

    CommandSplitter(String command) {
        this.command = command.split(" ")[0];
        this.args = parseArgs(command);
    }

    private List<String> parseArgs(String command) {
        command = command.substring(this.command.length() + 1);

        List<String> args = new ArrayList<>();
        boolean inquote = false;
        StringBuilder tmpString = new StringBuilder();

        if (StringUtils.isAllBlank(command)) return args;

        for (char c : command.toCharArray()) {
            if (c == '"') inquote = !inquote;
            else if (!inquote && c == ' ') {
                if (tmpString.length() > 0) {
                    args.add(tmpString.toString());
                    tmpString = new StringBuilder();
                }
            } else tmpString.append(c);
        }

        if (tmpString.length() > 0) args.add(tmpString.toString());

        return args;
    }
}

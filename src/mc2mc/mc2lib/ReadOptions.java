package mc2mc.mc2lib;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.ArrayList;
import java.util.List;

@Parameters(separators = "=")
public class ReadOptions {

    @Parameter(names={"-h","--help"}, description = "list all commands")
    public boolean isHelp = false;

    @Parameter(names={"-p","--display"}, description = "list all optimizations")
    public boolean isOptDisplay = false;

    @Parameter(names={"-a","--all"}, description = "Enable all optimization")
    public boolean isAll = true;

    @Parameter(names={"-e","--enable"}, arity = 1, description = "Enable all optimization")
    public boolean optEnable = true;

    @Parameter(names={"-d","--disable"}, arity = 1, description = "Disable all optimization")
    public boolean optDisable = false;

    @Parameter
    public List<String> arguments = new ArrayList<String>();
}

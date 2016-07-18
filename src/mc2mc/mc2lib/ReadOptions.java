package mc2mc.mc2lib;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.ArrayList;
import java.util.List;

@Parameters(separators = "=")
public class ReadOptions {

    @Parameter(names={"-h","--help"}, description = "List all commands")
    public boolean isHelp = false;

    @Parameter(names={"-args"}, description = "List all optimizations")
    public String arguments = ""; // pass program parameters to McLabCore

    @Parameter(names = {"-out"}, description = "Output directory")
    public String outDir = "";

    @Parameter(names = {"-tir"}, description = "Output tameIR")
    public boolean isTameIR = false;

    @Parameter(names = {"-plus"}, description = "TamePlus with check (default)")
    public boolean isPlus = false;

    @Parameter(names = {"-noplus"}, description = "Vectorized TameIR")
    public boolean isNoPlus = false;

//    @Parameter(names={"-p","--display"}, description = "list all optimizations")
//    public boolean isOptDisplay = false;

//    @Parameter(names={"-a","--all"}, description = "Enable all optimizations")
//    public boolean isAll = true;
//
//    @Parameter(names={"-e","--enable"}, arity = 1, description = "Enable an optimization")
//    public boolean optEnable = true;
//
//    @Parameter(names={"-d","--disable"}, arity = 1, description = "Disable all optimization")
//    public boolean optDisable = false;

    @Parameter(names={"-v","--view"}, description = "Tamer viewer")
    public boolean isOptViewer = false;

    @Parameter
    public List<String> inputArgs = new ArrayList<String>();
}

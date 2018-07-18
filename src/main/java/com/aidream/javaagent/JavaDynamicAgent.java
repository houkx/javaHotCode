package com.aidream.javaagent;

import java.lang.instrument.Instrumentation;

public class JavaDynamicAgent {

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("*** agentmain");
        System.getProperties().put(AgentTag.KEY_AGENT, inst);
    }
}

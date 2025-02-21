package sqlancer.mutable.jdbc;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class MutableJDBCConnectionB extends MutableJDBCAbstractConnection {

    MutableJDBCConnectionB(String path_to_binary, boolean debug) {
        this.systemWrapper = new MutableSystemWrapper();
        this.debug = debug;
        this.path_to_binary = path_to_binary;
    }

    @Override
    protected List<String> executeOnProcess(String sql) throws SQLException {
    	if (debug) System.out.println("MutableJDBCConnectionB#executeOnProcess: " + sql);
    	
    	
    	// Get all statements to execute
    	List<String> statements = new ArrayList<String>();
    	statements.add("CREATE DATABASE test;");
    	statements.add("USE test;");
    	
    	Map<String, MutableTableWrapper> tables = systemWrapper.getDatabaseWrapperInUse().getTables();
		for (MutableTableWrapper tbl : tables.values()) {
			statements.add(tbl.getCreate());
			statements.addAll(tbl.getAlteringStmts());
		}
		statements.add(sql);
		String statements_string = String.join(" ", statements);
		statements_string = statements_string.replace("'", "'\"'\"'");  // Escape single quotes
		
		
		// Create process
    	ProcessBuilder processBuilder = new ProcessBuilder();
        String shell = System.getProperty("os.name").contains("Mac OS") ? "/bin/zsh" : "/usr/bin/bash";
    	String cmd = "echo '" + statements_string + "' | " + path_to_binary + args; 
      processBuilder.command(shell, "-c", cmd);
//      processBuilder.command(path_to_binary, "-c", cmd);

        if (debug) System.out.println(cmd);
        
        
        // Start process and execute
    	try {
    		// Create process
//    	    long startTime = System.nanoTime();
        	Process process = processBuilder.start();
			process.waitFor(10, TimeUnit.SECONDS);		// Wait for termination, timeout 10s
//			long endTime = System.nanoTime();
			// System.out.println(String.format("%d", endTime - startTime));
			
        	// Get process output stream and reader
	         BufferedReader reader = new BufferedReader(
	                    new InputStreamReader(process.getInputStream()));
    		
    		List<String> out = new ArrayList<String>();
    		String line;
    		while(reader.ready() && (line = reader.readLine()) != null) {
    			out.add(line);
    		}
    		if (debug) System.out.println("\nOutput:");
    		if (debug) System.out.println(out);
    		
    		// Get process error stream and reader
    		reader = new BufferedReader(
    				new InputStreamReader(process.getErrorStream()));
    		List<String> err = new ArrayList<String>();
    		while(reader.ready() && (line = reader.readLine()) != null) {
    			err.add(line);
    		}
    		if (debug) System.out.println("\nError Stream:");
    		if (debug) System.out.println(err);
    		if (debug) System.out.println("\n\n");
    		
    		if (!err.isEmpty()) {
    		    throw new SQLException("Executing command\n" + cmd + "\nlead to error:\n" + err.get(0));
    		}

    		return out;
    	} catch (IOException e) {
            e.printStackTrace();
    		throw new SQLException("IOException: " +  e.getMessage());
    	}	catch (InterruptedException e) {
			e.printStackTrace();
			throw new SQLException("InterruptedException: " +  e.getMessage());
		}
    		
    }

    @Override
    public Statement createStatement() throws SQLException {
        return new MutableJDBCStatementB(this, debug);
    }

    @Override
    public void close() throws SQLException {
    	isClosed = true;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return isClosed;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
    	isClosed = true;
    }
    
}
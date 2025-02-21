package sqlancer.mutable.jdbc;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class MutableJDBCConnectionA extends MutableJDBCAbstractConnection {

    private final ProcessBuilder processBuilder;
    private Process process;
    OutputStream stdin;
    InputStream stdout;
    
    private boolean isClosed = false;
    private boolean debug;

    MutableJDBCConnectionA(String path_to_binary, boolean debug) {
        this.systemWrapper = new MutableSystemWrapper();
        String shell = System.getProperty("os.name").contains("Mac OS") ? "/bin/zsh" : "/usr/bin/bash";
        this.processBuilder = new ProcessBuilder().command(shell, "-c", path_to_binary + " --plan-enumerator PEall");
        this.debug = debug;
    }

    private void createNewProcess() throws IOException {
    	if (process != null ) {
    		if (process.isAlive()) process.destroy();
    	}
        process = processBuilder.start();

        // Add shutdown hook to close process when main program exits
		Thread shutdownHook = new Thread() {
		    public void run() {
		        process.destroy();
		    }
		};
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		
		// Get process input/output streams and writer/reader
//        this.stdin = process.getOutputStream();
//        this.stdout = process.getInputStream();
    }

    @Override
    protected List<String> executeOnProcess(String sql) throws SQLException {
    	if (debug) System.out.println("MutableJDBCConnectionA#executeOnProcess: " + sql);

    	try {
    		if (process == null || !process.isAlive()) { createNewProcess(); }
    		
    		OutputStream stdin = process.getOutputStream();
    	    InputStream stdout = process.getInputStream();;
    		
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
            BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));

            // Pass statement to process and execute
            writer.write(sql);
            writer.newLine();
            writer.flush();
            
            if (sql.startsWith("INSERT")) return new ArrayList<String>();
            
            List<String> out = new ArrayList<String>();
            List<String> err = new ArrayList<String>();
            boolean gotResult = false;
            boolean gotError = false;
            
            String line;
            while(!gotResult && !gotError) {
                //if (debug) System.out.println("waiting");
                TimeUnit.MILLISECONDS.sleep(2);
                if (reader.ready() && (line = reader.readLine()) != null) {
                    gotResult = true;
                    out.add(line);
                    break;
                }
                if (errorReader.ready() && (line = errorReader.readLine()) != null) {
                    gotError = true;
                    err.add(line);
                    break;
                }
            }
            
            // Read process error       
            while(errorReader.ready() && (line = errorReader.readLine()) != null) {
                err.add(line);
            }
            if (debug) System.out.println("\nError Stream:");
            if (debug) System.out.println(err);
            if (!err.isEmpty()) {
                try {
                    int i = process.exitValue();
                } catch (IllegalThreadStateException e) {
                }
                throw new SQLException("Error in subprocess: " + err);        
            }


    		// Read process output
    		while(reader.ready() && (line = reader.readLine()) != null) {
    			out.add(line);
    		}
    		
    		if (debug) System.out.println("\nOutput:");
            if (debug) System.out.println(out);
            if (debug) System.out.println("\n\n");

    		return out;
    	} catch (IOException e) {
            e.printStackTrace();
    		throw new SQLException("IOException: " +  e.getMessage());
    	} catch (InterruptedException e) {
            e.printStackTrace();
            throw new SQLException("InterruptedException: " +  e.getMessage());
        }
    }

    @Override
    public Statement createStatement() throws SQLException {
        if (this.process == null) {
            try {
                createNewProcess();
            } catch (IOException ex) {
                throw new SQLException(ex.toString());
            }
        }
        return new MutableJDBCStatementA(this, debug);
    }

    @Override
    public void close() throws SQLException {
    	if (isClosed) return;
    	if (process != null && process.isAlive()) process.destroy();
    	isClosed = true;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return isClosed;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
    	if (isClosed) return;
    	if (process != null && process.isAlive()) process.destroy();
    	isClosed = true;
    }
    
}

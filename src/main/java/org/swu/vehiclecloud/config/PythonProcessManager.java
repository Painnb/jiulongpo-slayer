package org.swu.vehiclecloud.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

@Component
public class PythonProcessManager implements InitializingBean, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(PythonProcessManager.class);
    private Process pythonProcess;
    private static final String PYTHON_SCRIPT_PATH = "pythonMLAnomaly/main.py";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    @Override
    public void afterPropertiesSet() {
        startPythonProcess();
    }

    private void startPythonProcess() {
        CompletableFuture.runAsync(() -> {
            int retries = 0;
            while (retries < MAX_RETRIES) {
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder("python", PYTHON_SCRIPT_PATH);
                    processBuilder.directory(new File(System.getProperty("user.dir")));
                    processBuilder.redirectErrorStream(true);
                    
                    logger.info("Starting Python FastAPI process...");
                    pythonProcess = processBuilder.start();

                    // 异步读取进程输出
                    new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()))
                            .lines()
                            .forEach(line -> logger.info("Python process output: {}", line));

                    // 检查进程是否正常启动
                    if (pythonProcess.isAlive()) {
                        logger.info("Python FastAPI process started successfully");
                        break;
                    }
                } catch (Exception e) {
                    logger.error("Failed to start Python process (attempt {}/{}): {}", 
                            retries + 1, MAX_RETRIES, e.getMessage());
                    retries++;
                    if (retries < MAX_RETRIES) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            if (retries == MAX_RETRIES) {
                logger.error("Failed to start Python process after {} attempts", MAX_RETRIES);
            }
        });
    }

    @Override
    public void destroy() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            logger.info("Shutting down Python FastAPI process...");
            pythonProcess.destroy();
            try {
                // 等待进程正常终止
                if (!pythonProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    // 如果进程未能在5秒内终止，强制终止
                    pythonProcess.destroyForcibly();
                }
                logger.info("Python FastAPI process terminated");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Error while shutting down Python process: {}", e.getMessage());
            }
        }
    }
}
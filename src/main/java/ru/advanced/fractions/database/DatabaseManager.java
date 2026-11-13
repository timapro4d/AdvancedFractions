package ru.advanced.fractions.database;
import java.io.File;import java.sql.*;import java.util.concurrent.*;
public class DatabaseManager {
 private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Fractions-SQLite");t.setDaemon(true);return t;});
 private Connection connection;
 public CompletableFuture<Void> init(File file){return CompletableFuture.runAsync(()->{try{
  file.getParentFile().mkdirs();connection=DriverManager.getConnection("jdbc:sqlite:"+file.getAbsolutePath());
  try(Statement s=connection.createStatement()){s.execute("PRAGMA journal_mode=WAL");s.execute("PRAGMA foreign_keys=ON");s.execute("PRAGMA busy_timeout=5000");
   s.executeUpdate("CREATE TABLE IF NOT EXISTS members(uuid TEXT PRIMARY KEY,name TEXT NOT NULL,fraction_id TEXT NOT NULL,rank INTEGER NOT NULL,warns INTEGER NOT NULL DEFAULT 0,duty INTEGER NOT NULL DEFAULT 0)");
   s.executeUpdate("CREATE TABLE IF NOT EXISTS ranks(fraction_id TEXT NOT NULL,rank INTEGER NOT NULL,name TEXT NOT NULL,PRIMARY KEY(fraction_id,rank))");
  }
 }catch(SQLException e){throw new CompletionException(e);}},executor);}
 public <T> CompletableFuture<T> query(SqlFunction<T> fn){return CompletableFuture.supplyAsync(()->{try{return fn.apply(connection);}catch(Exception e){throw new CompletionException(e);}},executor);}
 public CompletableFuture<Void> update(SqlConsumer fn){return CompletableFuture.runAsync(()->{try{fn.accept(connection);}catch(Exception e){throw new CompletionException(e);}},executor);}
 public void close(){try{if(connection!=null)connection.close();}catch(SQLException ignored){}executor.shutdown();}
 @FunctionalInterface public interface SqlFunction<T>{T apply(Connection c)throws Exception;}
 @FunctionalInterface public interface SqlConsumer{void accept(Connection c)throws Exception;}
}

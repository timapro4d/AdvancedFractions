package ru.advanced.fractions.manager;
import ru.advanced.fractions.database.DatabaseManager;import ru.advanced.fractions.model.FractionMember;
import java.sql.*;import java.util.*;import java.util.concurrent.*;
public class MemberManager {
 private final DatabaseManager db;private final Map<UUID,FractionMember> members=new ConcurrentHashMap<>();
 public MemberManager(DatabaseManager db){this.db=db;}
 public CompletableFuture<Void> load(){return db.query(c->{try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM members")){while(r.next()){FractionMember m=new FractionMember(UUID.fromString(r.getString("uuid")),r.getString("name"),r.getString("fraction_id"),r.getInt("rank"),r.getInt("warns"),r.getInt("duty")!=0);members.put(m.getUuid(),m);}}return null;});}
 public FractionMember get(UUID id){return members.get(id);} public Collection<FractionMember> all(){return List.copyOf(members.values());}
 public CompletableFuture<Void> add(FractionMember m){return db.update(c->{try(PreparedStatement p=c.prepareStatement("INSERT OR REPLACE INTO members(uuid,name,fraction_id,rank,warns,duty) VALUES(?,?,?,?,?,?)")){p.setString(1,m.getUuid().toString());p.setString(2,m.getName());p.setString(3,m.getFractionId());p.setInt(4,m.getRank());p.setInt(5,m.getWarns());p.setInt(6,m.isDuty()?1:0);p.executeUpdate();}}).thenRun(()->members.put(m.getUuid(),m));}
 public CompletableFuture<Void> remove(UUID id){return db.update(c->{try(PreparedStatement p=c.prepareStatement("DELETE FROM members WHERE uuid=?")){p.setString(1,id.toString());p.executeUpdate();}}).thenRun(()->members.remove(id));}
 public CompletableFuture<Void> changeRank(UUID id,int rank){FractionMember m=members.get(id);if(m==null)return CompletableFuture.failedFuture(new IllegalStateException("not member"));return db.update(c->{try(PreparedStatement p=c.prepareStatement("UPDATE members SET rank=? WHERE uuid=?")){p.setInt(1,rank);p.setString(2,id.toString());p.executeUpdate();}}).thenRun(()->m.setRank(rank));}
 public CompletableFuture<Integer> addWarn(UUID id){FractionMember m=members.get(id);if(m==null)return CompletableFuture.failedFuture(new IllegalStateException("not member"));int n=m.getWarns()+1;return db.update(c->{try(PreparedStatement p=c.prepareStatement("UPDATE members SET warns=? WHERE uuid=?")){p.setInt(1,n);p.setString(2,id.toString());p.executeUpdate();}}).thenApply(v->{m.setWarns(n);return n;});}
}

package ru.advanced.fractions.model;
import java.util.UUID;
public class FractionMember {
 private final UUID uuid; private final String name; private String fractionId; private int rank,warns; private boolean duty;
 public FractionMember(UUID uuid,String name,String fractionId,int rank,int warns,boolean duty){this.uuid=uuid;this.name=name;this.fractionId=fractionId;this.rank=rank;this.warns=warns;this.duty=duty;}
 public UUID getUuid(){return uuid;} public String getName(){return name;} public String getFractionId(){return fractionId;}
 public int getRank(){return rank;} public int getWarns(){return warns;} public boolean isDuty(){return duty;}
 public void setRank(int v){rank=v;} public void setWarns(int v){warns=v;} public void setDuty(boolean v){duty=v;}
}

package ru.advanced.fractions.model;
import java.util.*;
public class Fraction {
 private final String id, displayName; private final boolean governmentWave, department;
 private final List<String> ranks; private final List<Double> salaries;
 public Fraction(String id,String displayName,boolean wave,boolean department,List<String> ranks,List<Double> salaries){
  this.id=id;this.displayName=displayName;this.governmentWave=wave;this.department=department;
  this.ranks=new ArrayList<>(ranks);this.salaries=new ArrayList<>(salaries);
  while(this.ranks.size()<10)this.ranks.add("Ранг "+(this.ranks.size()+1));
  while(this.salaries.size()<10)this.salaries.add(0.0);
 }
 public String getId(){return id;} public String getDisplayName(){return displayName;}
 public boolean isGovernmentWave(){return governmentWave;} public boolean hasDepartmentAccess(){return department;}
 public String getRankName(int rank){return rank>=1&&rank<=10?ranks.get(rank-1):"Без ранга";}
 public double getSalary(int rank){return rank>=1&&rank<=10?salaries.get(rank-1):0;}
 public void setRankName(int rank,String name){if(rank>=1&&rank<=10)ranks.set(rank-1,name);}
 public List<String> getRanks(){return Collections.unmodifiableList(ranks);}
}

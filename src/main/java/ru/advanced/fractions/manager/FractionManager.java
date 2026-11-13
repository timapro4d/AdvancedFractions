package ru.advanced.fractions.manager;
import ru.advanced.fractions.model.Fraction;import org.bukkit.configuration.ConfigurationSection;import org.bukkit.configuration.file.FileConfiguration;
import java.util.*;
public class FractionManager {
 private final Map<String,Fraction> fractions=new LinkedHashMap<>();
 public FractionManager(FileConfiguration c){ConfigurationSection sec=c.getConfigurationSection("fractions");if(sec==null)return;
  for(String id:sec.getKeys(false)){String p="fractions."+id+".";List<String> ranks=c.getStringList(p+"ranks");List<Double> salaries=new ArrayList<>();for(Object o:c.getList(p+"salaries",List.of()))if(o instanceof Number n)salaries.add(n.doubleValue());
   fractions.put(id,new Fraction(id,c.getString(p+"display-name",id),c.getBoolean(p+"government-wave"),c.getBoolean(p+"department"),ranks,salaries));}
 }
 public Fraction get(String id){return fractions.get(id);} public Collection<Fraction> all(){return Collections.unmodifiableCollection(fractions.values());}
}

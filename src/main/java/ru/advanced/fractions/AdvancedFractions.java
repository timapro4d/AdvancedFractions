package ru.advanced.fractions;
import org.bukkit.plugin.java.JavaPlugin;import ru.advanced.fractions.database.DatabaseManager;import ru.advanced.fractions.manager.*;import ru.advanced.fractions.model.*;import ru.advanced.fractions.util.ColorUtil;
import org.bukkit.command.*;import org.bukkit.entity.Player;import java.io.File;import java.util.*;import java.util.concurrent.*;
public class AdvancedFractions extends JavaPlugin {
 private DatabaseManager database;private FractionManager fractions;private MemberManager members;private final Map<UUID,Invite> invites=new ConcurrentHashMap<>();
 @Override public void onEnable(){saveDefaultConfig();saveResource("messages.yml",false);database=new DatabaseManager();fractions=new FractionManager(getConfig());members=new MemberManager(database);
  database.init(new File(getDataFolder(),getConfig().getString("settings.database-file","fractions.db"))).thenCompose(v->members.load()).whenComplete((v,e)->getServer().getScheduler().runTask(this,()->{if(e!=null){getLogger().severe("Database init failed: "+e.getMessage());getServer().getPluginManager().disablePlugin(this);return;}register();getLogger().info("AdvancedFractions enabled.");}));
 }
 private void register(){for(String n:List.of("fraction","invite","faccept","fdeny","uninvite","giverank","fwarn","unfwarn","f","r","fb","rb","d","gov","freload","setleader")){PluginCommand c=getCommand(n);if(c!=null)c.setExecutor(this::execute);}}
 private boolean execute(CommandSender s,Command cmd,String label,String[] a){
  String n=cmd.getName().toLowerCase(Locale.ROOT);if(n.equals("freload")){if(!s.hasPermission("fractions.admin"))return msg(s,"no-permission");reloadConfig();fractions=new FractionManager(getConfig());s.sendMessage(col("&aКонфигурация перезагружена."));return true;}
  if(!(s instanceof Player p)){s.sendMessage("Only players.");return true;}FractionMember m=members.get(p.getUniqueId());
  switch(n){
   case "fraction": p.sendMessage(col("&6Доступные фракции: &f"+String.join(", ",fractions.all().stream().map(Fraction::getId).toList())));if(m!=null)p.sendMessage(col("&aВаша фракция: "+m.getFractionId()+" | ранг "+m.getRank()));return true;
   case "invite": if(a.length<1||m==null||m.getRank()<9)return msg(p,"not-member");Player target=getServer().getPlayerExact(a[0]);if(target==null)return msg(p,"player-not-found");if(members.get(target.getUniqueId())!=null)return msg(p,"not-member");invites.put(target.getUniqueId(),new Invite(p.getUniqueId(),m.getFractionId(),System.currentTimeMillis()+getConfig().getLong("settings.invite-expire-seconds",60)*1000));target.sendMessage(col("&e"+p.getName()+" приглашает вас во фракцию "+m.getFractionId()+". /faccept"));return true;
   case "faccept": Invite in=invites.remove(p.getUniqueId());if(in==null||in.expires<System.currentTimeMillis())return msg(p,"invite-expired");returnAsync(p,members.add(new FractionMember(p.getUniqueId(),p.getName(),in.fraction,1,0,false)),"&aВы вступили во фракцию "+in.fraction+".");return true;
   case "fdeny": invites.remove(p.getUniqueId());p.sendMessage(col("&eПриглашение отклонено."));return true;
   case "uninvite": if(a.length<1||m==null||m.getRank()<9)return msg(p,"not-member");OfflinePlayerLookup lookup=offline(a[0]);if(lookup==null)return msg(p,"player-not-found");returnAsync(p,members.remove(lookup.uuid),"&aИгрок уволен.");return true;
   case "giverank": if(a.length<2||m==null||m.getRank()<9)return msg(p,"not-member");OfflinePlayerLookup l=offline(a[0]);if(l==null)return msg(p,"player-not-found");try{int rank=Integer.parseInt(a[1]);if(rank<1||rank>10||rank>=m.getRank())return msg(p,"no-permission");returnAsync(p,members.changeRank(l.uuid,rank),"&aРанг обновлён.");}catch(NumberFormatException ex){p.sendMessage(col("&cРанг должен быть числом 1-10."));}return true;
   case "fwarn": if(a.length<1||m==null||m.getRank()<9)return msg(p,"not-member");OfflinePlayerLookup w=offline(a[0]);if(w==null)return msg(p,"player-not-found");members.addWarn(w.uuid).whenComplete((count,e)->getServer().getScheduler().runTask(this,()->p.sendMessage(col(e==null?"&aВыговор выдан. Всего: "+count:"&cОшибка БД."))));return true;
   case "f","r","fb","rb","d": if(m==null)return msg(p,"not-member");if(a.length==0)return true;String text=String.join(" ",a);for(Player online:getServer().getOnlinePlayers()){FractionMember om=members.get(online.getUniqueId());if(om!=null&&(n.equals("d")?fractions.get(m.getFractionId()).hasDepartmentAccess():om.getFractionId().equals(m.getFractionId())))online.sendMessage(col("&8["+m.getFractionId()+"] &f"+p.getName()+": "+text));}return true;
   case "gov": if(m==null||m.getRank()<9)return msg(p,"no-permission");String announcement=String.join(" ",a);if(announcement.isBlank())return true;for(Player online:getServer().getOnlinePlayers())online.sendMessage(col("&6[ГОСВОЛНА] &f"+p.getName()+": "+announcement));return true;
   case "setleader": if(!p.hasPermission("fractions.admin")||a.length<2)return msg(p,"no-permission");OfflinePlayerLookup leader=offline(a[0]);if(leader==null)return msg(p,"player-not-found");returnAsync(p,members.add(new FractionMember(leader.uuid,a[0],a[1],10,0,false)),"&aЛидер назначен.");return true;
  }return true;
 }
 private record Invite(UUID inviter,String fraction,long expires){} private record OfflinePlayerLookup(UUID uuid){}
private OfflinePlayerLookup offline(String name) {
    org.bukkit.OfflinePlayer op = getServer().getOfflinePlayer(name);
    return op == null ? null : new OfflinePlayerLookup(op.getUniqueId());
}
 private void returnAsync(Player p,CompletableFuture<Void> f,String success){f.whenComplete((v,e)->getServer().getScheduler().runTask(this,()->p.sendMessage(col(e==null?success:"&cОперация не выполнена: ошибка базы данных."))));}
 private boolean msg(CommandSender s,String key){s.sendMessage(col(getConfig().getString("messages.prefix","")+getConfig().getString("messages."+key,"&cОшибка.")));return true;}
 private String col(String s){return ColorUtil.color(s);} public FractionManager getFractions(){return fractions;}public MemberManager getMembers(){return members;}
 @Override public void onDisable(){if(database!=null)database.close();}
}

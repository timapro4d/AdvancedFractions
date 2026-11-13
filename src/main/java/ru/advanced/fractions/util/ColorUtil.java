package ru.advanced.fractions.util;
import net.md_5.bungee.api.ChatColor;
import java.util.regex.*;
public final class ColorUtil {
 private static final Pattern HEX=Pattern.compile("&#([A-Fa-f0-9]{6})");
 private ColorUtil(){}
 public static String color(String s){
  if(s==null)return "";
  Matcher m=HEX.matcher(s); StringBuffer b=new StringBuffer();
  while(m.find())m.appendReplacement(b, ChatColor.of("#"+m.group(1)).toString());
  m.appendTail(b); return ChatColor.translateAlternateColorCodes('&',b.toString());
 }
}

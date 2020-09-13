package fr.milekat.discord.obj;

import java.util.ArrayList;

public class Team {
    private int id;
    private String name;
    private String tag;
    private int money;
    private ArrayList<String> members;
    private int size;

    public Team(int id, String name, String tag, int money, ArrayList<String> members1, int members) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.money = money;
        this.members = members1;
        this.size = members;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSize() {
        return size;
    }

    public void addSize(int add) {
        this.size = size+add;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public ArrayList<String> getMembers() {
        return members;
    }

    public void addMembers(String member) {
        this.members.add(member);
    }

    public void removeMembers(String member) {
        this.members.remove(member);
    }

    public void setMembers(ArrayList<String> members) {
        this.members = members;
    }
}

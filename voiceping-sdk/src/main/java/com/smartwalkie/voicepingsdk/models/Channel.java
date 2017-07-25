package com.smartwalkie.voicepingsdk.models;


import java.util.List;


public class Channel  {

    private String name;
    private int id;
    private List<User> users;
    private boolean isFavorite;

    public Channel() {
    }

    public Channel(int id, String name) {
        this.name = name;
        this.id = id;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Channel)){
            return false;
        }
        Channel channel=(Channel)o;
        return channel.getId()==id;
    }
}

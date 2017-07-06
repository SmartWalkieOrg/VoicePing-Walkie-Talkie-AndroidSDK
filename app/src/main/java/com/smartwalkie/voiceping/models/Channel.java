package com.smartwalkie.voiceping.models;


import java.util.List;


public class Channel  {

    String name;
    int id;

    List<User> users;

    boolean isFavoritel;

    public boolean isFavorite() {
        return isFavoritel;
    }

    public void setFavorite(boolean isFavoritel) {
        this.isFavoritel = isFavoritel;
    }

    public Channel() {
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<User> getUsers() {
        return users;
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



    public Channel(int id, String name) {
        this.name = name;
        this.id = id;
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

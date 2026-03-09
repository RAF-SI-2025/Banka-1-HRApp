package com.example.model;

import java.util.ArrayList;
import java.util.List;

public class TeamMember {
    private long id;
    private String name;
    private String surname;
    private List<Task> tasks;
    private List<String> skills;
    private List<Integer> grades;

    public TeamMember(String name, String surname) {
        this.name = name;
        this.surname = surname;
        this.grades = new ArrayList<>();
        this.tasks = new ArrayList<>();
        this.skills = new ArrayList<>();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public double getAverageGrade(){
        if (grades.isEmpty()) return 0;
        double result = 0;
        for(Integer grade : grades){
            result += grade;
        }
        return result / grades.size();
    }

    public void addTask(Task task){
        task.setTeamMember(this);
        tasks.add(task);
    }

    public void completedTask(Task task){
        if(tasks.contains(task)){
            task.setStatus(TaskStatus.COMPLETED);
            System.out.println("User " + this.name + " " + this.surname + " has completed task " + task.getTaskName());
        }
    }
    public void failedTask(Task task){
        if(tasks.contains(task)) {
            task.setStatus(TaskStatus.FAILED);
            System.out.println("User " + this.name + " " + this.surname + " has failed task " + task.getTaskName());
        }
    }

    public void addSkill(String skill){
        if(!skills.contains(skill.toUpperCase().trim())) {
            skills.add(skill.toUpperCase().trim());
        }
        else{
            System.out.println("Team member " + this.name + " " + this.surname + " already has skill: " + skill.toUpperCase().trim());
        }
    }

    public void addGrade(Integer grade){
        System.out.println("Adding grade: " + grade + " to user " + this.name + " " + this.surname);
        this.grades.add(grade);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<Integer> getGrades() {
        return grades;
    }

    public void setGrades(List<Integer> grades) {
        this.grades = grades;
    }
}

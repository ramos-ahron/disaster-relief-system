# Disaster Relief Management System

A Java 17 application for relief workers after a disaster. It tracks the people affected, where they are, the supplies at each shelter, medical treatment, dietary needs, relationships between people and calls from relatives looking for someone.

It has two parts that share one PostgreSQL database:

- A desktop app (Swing) with one tab for central relief workers and one for workers at a location.
- An HTTP API for the family graph: who is whose parent, and who is or was a couple.

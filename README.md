Bachelorarbeit
==============

Realtime Raytracing

This program was written for my Bachelor's dissertation on Realtime Raytracing.
It features three modes: 

1. Simple Ray Casting with Shadow Rays

2. Recursive Raytracing (only refraction rays)

3. Recursive Raytracing (only reflection rays)

=================
Usage:

To flip thru these modes, press M.


To add new objects, proceed as followed:

INSERT - add Sphere

HOME - add Triangle Vertex, Triangle spawn at the count of 3

LEFT/RIGHT - flip thru the parameters

UP/DOWN - change the currently selected parameter

R - toggle random values for parameters

T - toggle Triangle-Strip-Mode


DELETE - removes the object in the center (only in mode 1)

=======================




To run this code first install OpenCL for the GPU-device on your system.
For NVIDIA-cards this is usually already installed with the driver.
Also make sure JRE 6 is installed.


For Windows: 
Grab the executable Jar-file Realtime_Raytracing.jar and double click on it.


For Linux: 
Open the Terminal and locate the Jar-File. Run with

% java -jar Realtime_Raytracing.jar

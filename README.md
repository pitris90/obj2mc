# OBJ2MC / OBJ2SCHEMATIC 

Tool for converting OBJ file into Minecraft in 1:1 ratio. That means your OBJ model should have max 256 metres on height, because of minecraft height limit. 

# How to use
1. Clone this repo in your pc
2. Open in src/main/java/municrafi/obj2mc Schematic.java file
  a) scroll down to main function (row around 240) 
  b) change source address of your OBJ file in "new FileInputStream()" e.g. from "new FileInputStream("C:/folder/test.obj")" to "new FileInputStream("C:/Users/me/Documents/myobj.obj")"
  c) change output destination of schematic file in "new File()" (similiar to previous step, only change it to address where you want your schematic file e.g. "new File("C:/Users/me/Document/myschem.schematic")"
3. Run the program in cmd
4. Enjoy your schematic!

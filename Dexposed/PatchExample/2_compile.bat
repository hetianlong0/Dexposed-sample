::��src�µ��ļ�·������src.list�ļ���
::find src name *.java > src.list
 
::��gen�µ��ļ�·������gen.list�ļ���
::find gen name *.java > gen.list 

mkdir bin

javac -target 1.6 -bootclasspath D:/android-sdk-windows/platforms/android-17/android.jar -d bin gen\com\example\antdemo\*.java src\com\example\antdemo\*.java
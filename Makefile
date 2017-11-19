default:
	javac -cp ".:libs/jersey-bundle-1.17.1.jar:libs/servlet-api.jar:libs/jersey-multipart-1.17.1.jar:libs/mimepull-1.6.jar:/usr/share/tomcat8/lib/servlet-api.jar" Server.java
	javac -cp ".:libs/jersey-bundle-1.17.1.jar:libs/servlet-api.jar:libs/jersey-multipart-1.17.1.jar:libs/mimepull-1.6.jar:/usr/share/tomcat8/lib/servlet-api.jar" IChordNodeServer.java
	cp Server.class myapp/WEB-INF/classes
	cp IChordNodeServer.class myapp/WEB-INF/classes
	javac -cp ".:libs/jersey-bundle-1.17.1.jar:libs/jersey-multipart-1.17.1.jar" ChordNode.java

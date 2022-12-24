package com.cdac.enrollmentstation.security;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
/**
 * 
 * @author K. Karthikeyan
 *
 */

public class OpensslAESUtil {
	public static void opensslDESdec(String inputPath, String outputPath, String pass) {
		try {
			String cmd = "openssl enc -d -A -aes-256-cbc -in "+inputPath+" -out "+outputPath+" -base64 -pass pass:"+pass;
			System.out.println(cmd);
			Process proc=Runtime.getRuntime().exec(cmd);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			
			//Read the output from the command
			System.out.println("Output of the command1:\n");
			String s = null;
			while ((s = stdInput.readLine()) != null) {
			    System.out.println(s);
			}
			
			//Read errors from the attempted command
			System.out.println("Here is the standard error of the command1 (if any):\n");
			while ((s = stdError.readLine()) != null) {
			    System.out.println(s);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}			
}

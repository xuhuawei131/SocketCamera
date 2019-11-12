package com.xzy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SocketCameraActivity extends Activity implements SurfaceHolder.Callback,
Camera.PreviewCallback{		
	private SurfaceView mSurfaceview = null; // SurfaceView����(��ͼ���)��Ƶ��ʾ
    private SurfaceHolder mSurfaceHolder = null; // SurfaceHolder����(����ӿ�)SurfaceView֧����
    private Camera mCamera = null; // Camera�������Ԥ�� 
    
    /**��������ַ*/
    private String pUsername="XZY";
    /**��������ַ*/
    private String serverUrl="192.168.1.100";
    /**�������˿�*/
    private int serverPort=8888;
    /**��Ƶˢ�¼��*/
    private int VideoPreRate=1;
    /**��ǰ��Ƶ���*/
    private int tempPreRate=0;
    /**��Ƶ����*/
    private int VideoQuality=85;
    
    /**������Ƶ��ȱ���*/
    private float VideoWidthRatio=1;
    /**������Ƶ�߶ȱ���*/
    private float VideoHeightRatio=1;
    
    /**������Ƶ���*/
    private int VideoWidth=320;
    /**������Ƶ�߶�*/
    private int VideoHeight=240;
    /**��Ƶ��ʽ����*/
    private int VideoFormatIndex=0;
    /**�Ƿ�����Ƶ*/
    private boolean startSendVideo=false;
    /**�Ƿ���������*/
    private boolean connectedServer=false;
    
    private Button myBtn01, myBtn02;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //��ֹ��Ļ����
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, 
        		WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
               
        mSurfaceview = (SurfaceView) findViewById(R.id.camera_preview);
        myBtn01=(Button)findViewById(R.id.button1);
        myBtn02=(Button)findViewById(R.id.button2);
                
        //��ʼ����������ť
        myBtn01.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				//Common.SetGPSConnected(LoginActivity.this, false);
				if(connectedServer){//ֹͣ����������ͬʱ�Ͽ�����
					startSendVideo=false;
					connectedServer=false;					
					myBtn02.setEnabled(false);
					myBtn01.setText("��ʼ����");
					myBtn02.setText("��ʼ����");
					//�Ͽ�����
					Thread th = new MySendCommondThread("PHONEDISCONNECT|"+pUsername+"|");
			  	  	th.start(); 
				}
				else//��������
				{
					//�����̷߳�������PHONECONNECT
			  	  	Thread th = new MySendCommondThread("PHONECONNECT|"+pUsername+"|");
			  	  	th.start(); 
					connectedServer=true;
					myBtn02.setEnabled(true);
					myBtn01.setText("ֹͣ����");
				}
			}});
        
        myBtn02.setEnabled(false);
        myBtn02.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				if(startSendVideo)//ֹͣ������Ƶ
				{
					startSendVideo=false;
					myBtn02.setText("��ʼ����");
				}
				else{ // ��ʼ������Ƶ
					startSendVideo=true;
					myBtn02.setText("ֹͣ����");
				}
			}});
    }
    
    @Override
    public void onStart()//����������ʱ��
    {	
    	mSurfaceHolder = mSurfaceview.getHolder(); // ��SurfaceView��ȡ��SurfaceHolder����
    	mSurfaceHolder.addCallback(this); // SurfaceHolder����ص��ӿ�       
    	mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// ������ʾ�����ͣ�setType��������
	    //��ȡ�����ļ�
        SharedPreferences preParas = PreferenceManager.getDefaultSharedPreferences(SocketCameraActivity.this);
        pUsername=preParas.getString("Username", "XZY");
        serverUrl=preParas.getString("ServerUrl", "192.168.0.100");
    	String tempStr=preParas.getString("ServerPort", "8888");
    	serverPort=Integer.parseInt(tempStr);
        tempStr=preParas.getString("VideoPreRate", "1");
        VideoPreRate=Integer.parseInt(tempStr);	            
        tempStr=preParas.getString("VideoQuality", "85");
        VideoQuality=Integer.parseInt(tempStr);
        tempStr=preParas.getString("VideoWidthRatio", "100");
        VideoWidthRatio=Integer.parseInt(tempStr);
        tempStr=preParas.getString("VideoHeightRatio", "100");
        VideoHeightRatio=Integer.parseInt(tempStr);
        VideoWidthRatio=VideoWidthRatio/100f;
        VideoHeightRatio=VideoHeightRatio/100f;
        
        super.onStart();
    }
    
    @Override
    protected void onResume() {
        super.onResume();        
        InitCamera();
    }
    
    /**��ʼ������ͷ*/
    private void InitCamera(){
    	try{
    		mCamera = Camera.open();
    	} catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    @Override
    protected void onPause() {
        super.onPause();
        try{
	        if (mCamera != null) {
	        	mCamera.setPreviewCallback(null); // �������������ǰ����Ȼ�˳�����
	            mCamera.stopPreview();
	            mCamera.release();
	            mCamera = null;
	        } 
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }
	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		if (mCamera == null) {
            return;
        }
        mCamera.stopPreview();
        mCamera.setPreviewCallback(this);
        mCamera.setDisplayOrientation(90); //���ú���¼��
        //��ȡ����ͷ����
        Camera.Parameters parameters = mCamera.getParameters();
        Size size = parameters.getPreviewSize();
        VideoWidth=size.width;
        VideoHeight=size.height;
        VideoFormatIndex=parameters.getPreviewFormat();
        
        mCamera.startPreview();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } 
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		if (null != mCamera) {
            mCamera.setPreviewCallback(null); // �������������ǰ����Ȼ�˳�����
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// TODO Auto-generated method stub
		//���û��ָ�����Ƶ�����Ȳ���
		if(!startSendVideo)
			return;
		if(tempPreRate<VideoPreRate){
			tempPreRate++;
			return;
		}
		tempPreRate=0;		
		try {
		      if(data!=null)
		      {
		        YuvImage image = new YuvImage(data,VideoFormatIndex, VideoWidth, VideoHeight,null);
		        if(image!=null)
		        {
		        	ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		      	  	//�ڴ�����ͼƬ�ĳߴ������ 
		      	  	image.compressToJpeg(new Rect(0, 0, (int)(VideoWidthRatio*VideoWidth), 
		      	  		(int)(VideoHeightRatio*VideoHeight)), VideoQuality, outstream);  
		      	  	outstream.flush();
		      	  	//�����߳̽�ͼ�����ݷ��ͳ�ȥ
		      	  	Thread th = new MySendFileThread(outstream,pUsername,serverUrl,serverPort);
		      	  	th.start();  
		        }
		      }
		  } catch (IOException e) {
		      e.printStackTrace();
		  }
	}
	    
    /**�����˵�*/    
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	menu.add(0,0,0,"ϵͳ����");
    	menu.add(0,1,1,"���ڳ���"); 
    	menu.add(0,2,2,"�˳�����"); 
    	return super.onCreateOptionsMenu(menu);
    }
    /**�˵�ѡ��ʱ��������Ӧ�¼�*/  
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	super.onOptionsItemSelected(item);//��ȡ�˵�
    	switch(item.getItemId())//�˵����
    	{
    		case 0:
    			//ϵͳ����
    			{
    				Intent intent=new Intent(this,SettingActivity.class);
    				startActivity(intent);  
    			}
    			break;  
    		case 1://���ڳ���
    		{
    			new AlertDialog.Builder(this)
    			.setTitle("���ڱ�����")
    			.setMessage("���������人��ѧˮ��ˮ��ѧԺФ������ơ���д��\nEmail��xwebsite@163.com")
    			.setPositiveButton
    			(
    				"��֪����",
    				new DialogInterface.OnClickListener()
    				{						
    					@Override
    					public void onClick(DialogInterface dialog, int which) 
    					{
    					}
    				}
    			)
    			.show();
    		}
			break;
    		case 2://�˳�����
	    		{
	    			//ɱ���߳�ǿ���˳�
					android.os.Process.killProcess(android.os.Process.myPid());
	    		}
    			break;
    	}    	
    	return true;
    }
    
    /**���������߳�*/
    class MySendCommondThread extends Thread{
    	private String commond;
    	public MySendCommondThread(String commond){
    		this.commond=commond;
    	}
    	public void run(){
    		//ʵ����Socket  
            try {
    			Socket socket=new Socket(serverUrl,serverPort);
    			PrintWriter out = new PrintWriter(socket.getOutputStream());
    			out.println(commond);
    			out.flush();
    		} catch (UnknownHostException e) {
    		} catch (IOException e) {
    		}  
    	}
    }
    
    /**�����ļ��߳�*/
    class MySendFileThread extends Thread{	
    	private String username;
    	private String ipname;
    	private int port;
    	private byte byteBuffer[] = new byte[1024];
    	private OutputStream outsocket;	
    	private ByteArrayOutputStream myoutputstream;
    	
    	public MySendFileThread(ByteArrayOutputStream myoutputstream,String username,String ipname,int port){
    		this.myoutputstream = myoutputstream;
    		this.username=username;
    		this.ipname = ipname;
    		this.port=port;
            try {
    			myoutputstream.close();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	}
    	
        public void run() {
            try{
            	//��ͼ������ͨ��Socket���ͳ�ȥ
                Socket tempSocket = new Socket(ipname, port);
                outsocket = tempSocket.getOutputStream();
                //д��ͷ��������Ϣ
            	String msg=java.net.URLEncoder.encode("PHONEVIDEO|"+username+"|","utf-8");
                byte[] buffer= msg.getBytes();
                outsocket.write(buffer);
                
                ByteArrayInputStream inputstream = new ByteArrayInputStream(myoutputstream.toByteArray());
                int amount;
                while ((amount = inputstream.read(byteBuffer)) != -1) {
                    outsocket.write(byteBuffer, 0, amount);
                }
                myoutputstream.flush();
                myoutputstream.close();
                tempSocket.close();                   
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
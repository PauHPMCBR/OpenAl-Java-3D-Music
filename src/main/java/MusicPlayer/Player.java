package MusicPlayer;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static java.lang.Math.*;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.SOFTHRTF.*;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename;
import static org.lwjgl.system.MemoryStack.stackPush;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;

/** Simple HRTF demo. Ported from <a href="https://github.com/kcat/openal-soft/blob/master/examples/alhrtf.c">alhrtf.c</a>. */
//https://javadoc.lwjgl.org/org/lwjgl/openal/SOFTHRTF.html
//https://github.com/LWJGL/lwjgl3/blob/a9ec74f2d7c831b4d6600af0860f49be236f255b/modules/core/src/test/java/org/lwjgl/demo/openal/HRTFDemo.java

public class Player {

    public static JFrame createGUI() throws IOException {
        JFrame frame = new JFrame("Mouse Position Detector");
        Image img = ImageIO.read(ClassLoader.getSystemResource("note.png"));
        frame.setIconImage(img);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000,1000);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.add(new JPanel() {
            public void paint(Graphics g) {
                setSize(1000,1000);
                g.drawOval(490,460,10,10);
                g.setColor(Color.RED);
                g.fillOval(490,460,10,10);
            }
        });
        return frame;
    }

    public static void main(String[] args) throws Exception {
        String soundname = "sound.mp3";
        if (args.length > 0) {
            soundname = args[0];
        }
        JFrame frame = createGUI();

        //create device for output
        long device = alcOpenDevice((ByteBuffer)null);
        ALCCapabilities deviceCaps = ALC.createCapabilities(device);
        long context = alcCreateContext(device, (IntBuffer)null);
        alcMakeContextCurrent(context);
        AL.createCapabilities(deviceCaps);

        IntBuffer attr = BufferUtils.createIntBuffer(10).put(ALC_HRTF_SOFT).put(ALC_TRUE);
        attr.put(0);
        attr.flip();
        /* Load the sound into a buffer. */
        int channels;
        int sampleRate;
        int bufferPointer;
        int format;
        ShortBuffer pcm;
        bufferPointer = alGenBuffers();
        WaveLoader wave;
        if (soundname.endsWith("ogg")) {
            try (MemoryStack stack = stackPush()) {
                IntBuffer channelsBuffer = stack.mallocInt(1);
                IntBuffer sampleRateBuffer = stack.mallocInt(1);


                pcm = stb_vorbis_decode_filename(soundname, channelsBuffer, sampleRateBuffer);

                channels = channelsBuffer.get(0);
                sampleRate = sampleRateBuffer.get(0);


                format = -1;
                if (channels == 1) {
                    format = AL_FORMAT_MONO16;
                } else if (channels == 2) {
                    format = AL_FORMAT_STEREO16;
                }
            }
        }
        else {
            if (soundname.endsWith("mp3")) {
                //https://programmersought.com/article/8749987712/
                if (!new File(soundname + ".wav").exists()) {
                    final ByteArrayInputStream bais = new ByteArrayInputStream(getBytes(soundname));
                    final AudioInputStream sourceAIS = AudioSystem.getAudioInputStream(bais);
                    AudioFormat sourceFormat = sourceAIS.getFormat();
                    AudioFormat mp3tFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false);
                    AudioFormat pcmFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 1, 2, 16000, false);
                    final AudioInputStream mp3AIS = AudioSystem.getAudioInputStream(mp3tFormat, sourceAIS);
                    final AudioInputStream pcmAIS = AudioSystem.getAudioInputStream(pcmFormat, mp3AIS);
                    AudioSystem.write(pcmAIS, AudioFileFormat.Type.WAVE, new File(soundname + ".wav"));
                }
                soundname += ".wav";
            }
            wave = new WaveLoader(soundname);
            format = wave.getFormat();
            pcm = wave.getData().asShortBuffer();
            sampleRate = wave.getSampleRate();
        }

        alBufferData(bufferPointer, format, pcm, sampleRate);
        /* Create the source to play the sound with. */
        int source = alGenSources();
        alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE);
        alSourcei(source, AL_BUFFER, bufferPointer);
        alSourcei(source, AL_LOOPING, AL_TRUE);

        /* Play the sound until it finishes. */
        alSourcePlay(source);
        int state;
        do {
            try { Thread.sleep(1); }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            int x = frame.getMousePosition().x;
            int y = frame.getMousePosition().y;
            float x1 = -(float)(500-x)/125;
            float y1 = -(float)(500-y)/125;
            x1 *= sin(abs(x1)/4*Math.PI/2);
            y1 *= sin(abs(y1)/4*Math.PI/2);

            alSource3f(source, AL_POSITION, x1, 0.0f, y1);
            state = alGetSourcei(source, AL_SOURCE_STATE);
        } while ( alGetError() == AL_NO_ERROR && state == AL_PLAYING );

        /* All done. Delete resources, and close OpenAL. */
        alDeleteSources(source);
        alDeleteBuffers(bufferPointer);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    private static byte[] getBytes(String filePath) {
        byte[] buffer = null;
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
            byte[] b = new byte[1000];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }
}
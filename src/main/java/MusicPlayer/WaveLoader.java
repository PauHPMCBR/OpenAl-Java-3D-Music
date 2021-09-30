package MusicPlayer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.lwjgl.openal.AL10;

//https://www.programcreek.com/java-api-examples/?code=sunenielsen%2Ftribaltrouble%2Ftribaltrouble-master%2Ftt%2Fclasses%2Fcom%2Foddlabs%2Ftt%2Faudio%2FAudio.java#
public final strictfp class WaveLoader {
    private ByteBuffer data;
    private int format;
    private int sample_rate;

    public WaveLoader(String file) throws UnsupportedAudioFileException, IOException {
        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(file));
        AudioFormat audio_format = ais.getFormat();
        format = getFormat(audio_format.getChannels(), audio_format.getSampleSizeInBits());

        byte[] temp_buffer = new byte[audio_format.getChannels()*(int)ais.getFrameLength()*audio_format.getSampleSizeInBits()/8];
        int read = 0;
        int total = 0;
        while ((total < temp_buffer.length) && (read = ais.read(temp_buffer, total, temp_buffer.length - total)) != -1) {
            total += read;
        }

        data = directWaveOrder(temp_buffer, audio_format.getSampleSizeInBits());
        sample_rate = (int)audio_format.getSampleRate();
        ais.close();
    }




    public WaveLoader(ByteBuffer data, int channels, int bitrate, int sample_rate) {
        this.data = data;
        this.sample_rate = sample_rate;
        format = getFormat(channels, bitrate);
    }

    public WaveLoader() {
    }

    public final static int getFormat(int channels, int sample_size_in_bits) {
        if (channels == 1 && sample_size_in_bits == 8)
            return AL10.AL_FORMAT_MONO8;
        else if (channels == 1 && sample_size_in_bits == 16)
            return AL10.AL_FORMAT_MONO16;
        else if (channels == 2 && sample_size_in_bits == 8)
            return AL10.AL_FORMAT_STEREO8;
        else if (channels == 2 && sample_size_in_bits == 16)
            return AL10.AL_FORMAT_STEREO16;
        else
            throw new RuntimeException("Unsupported wave format");
    }

    private final ByteBuffer directWaveOrder(byte[] buffer, int bits) {
        ByteBuffer src = ByteBuffer.wrap(buffer);
        src.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer dest = ByteBuffer.allocateDirect(buffer.length);
        dest.order(ByteOrder.nativeOrder());

        if (bits == 16) {
            ShortBuffer dest_short = dest.asShortBuffer();
            ShortBuffer src_short = src.asShortBuffer();
            while (src_short.hasRemaining())
                dest_short.put(src_short.get());
        } else {
            while (src.hasRemaining())
                dest.put(src.get());
        }
        dest.rewind();
        return dest;
    }


    public final ByteBuffer getData() {
        return data;
    }

    public final int getFormat() {
        return format;
    }

    public final int getSampleRate() {
        return sample_rate;
    }
}
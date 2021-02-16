package net.roguelogix.phosphophyllite.util;

public class HeatBody {
    
    public boolean isInfinite = false;
    public double rfPerKelvin;
    public double temperature;
    
    /**
     * bi directional heat transfer between two heat bodies
     *
     * @param other other heat body
     * @param rfkt  RF per kelvin tick, basically watts per metre squared kelvin
     */
    public void transferWith(HeatBody other, double rfkt) {
        // cant transfer between two infinite bodies
        if (isInfinite && other.isInfinite) {
            return;
        }
        if (!isInfinite && other.isInfinite) {
            // i dont want to implement it for both ways, so im just gonna assume that if one of them is infinite, its this one
            other.transferWith(this, rfkt);
            return;
        }
        
        if (!isInfinite) {
            
            double targetTemperature = ((this.rfPerKelvin * (this.temperature - other.temperature)) / (this.rfPerKelvin + other.rfPerKelvin));
            targetTemperature += other.temperature;
            
            double denominator = rfkt * (this.rfPerKelvin + other.rfPerKelvin);
            denominator /= this.rfPerKelvin * other.rfPerKelvin;
            denominator = Math.exp(-denominator);
            
            double thisNewTemp = this.temperature - targetTemperature;
            thisNewTemp *= denominator; // its flipped in the exp
            thisNewTemp += targetTemperature;
    
            double otherNewTemp = other.temperature - targetTemperature;
            otherNewTemp *= denominator; // its flipped in the exp
            otherNewTemp += targetTemperature;
            
            this.temperature = thisNewTemp;
            other.temperature = otherNewTemp;
            
        } else {
            
            double newTemp = other.temperature - temperature;
            newTemp *= Math.exp(-rfkt / other.rfPerKelvin);
            newTemp += temperature;
            
            other.temperature = newTemp;
        }
    }
    
    public double additionalRFForTemperature(double targetTemperature) {
        double currentRF = rfFromTemperature(temperature);
        double targetRF = rfFromTemperature(targetTemperature);
        return targetRF - currentRF;
    }
    
    public double temperatureWithAdditionalRF(double rf) {
        return temperature + (isInfinite ? 0 : rf / (rfPerKelvin));
    }
    
    public void absorbRF(double rf) {
        this.temperature = temperatureWithAdditionalRF(rf);
    }
    
    public double rfFromTemperature(double temperature) {
        return temperature * rfPerKelvin;
    }
    
    public double rf() {
        return rfFromTemperature(temperature);
    }
    
    public double tempFromRF(double rf) {
        return rf / (rfPerKelvin);
    }
}

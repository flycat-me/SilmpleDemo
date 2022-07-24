package org.camunda.silmple.demo;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import java.util.Random;

/**
 * @author flycat
 * @since 2022-07-24 12:00
 **/
public class CheckWeatherDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        Random random = new Random();
        delegateExecution.setVariable("name","flycat");
        delegateExecution.setVariable("weatherOk",random.nextBoolean());
    }
}

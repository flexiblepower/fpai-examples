package org.flexiblepower.simulation.dishwasher;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import org.flexiblepower.context.FlexiblePowerContext;
import org.flexiblepower.ral.drivers.dishwasher.DishwasherControlParameters;
import org.flexiblepower.ral.drivers.dishwasher.DishwasherState;
import org.flexiblepower.ui.Widget;

public class DishwasherWidget implements Widget {

    public static class Update {
        private final String program;
        private final String date;

        public Update(String program, Date date) {
            this.program = program;
            if (date != null) {
                this.date = date.toString();
            } else {
                this.date = "None";
            }
        }

        public String getProgram() {
            return program;
        }

        public String getDate() {
            return date;
        }
    }

    private final DishwasherSimulation dishwasher;
    private final FlexiblePowerContext context;

    public DishwasherWidget(DishwasherSimulation dishwasher, FlexiblePowerContext context) {
        this.dishwasher = dishwasher;
        this.context = context;

    }

    public DishwasherSimulation getDishwasher() {
        return dishwasher;
    }

    private DishwasherState state;

    public Update update(Locale locale) {
        state = dishwasher.getCurrentState();
        if (state != null) {
            return new Update(state.getProgram(), state.getStartTime());
        } else {
            return new Update("No Program Selected", null);
        }
    }

    public Update startProgram(Locale locale) throws IOException {
        dishwasher.handleControlParameters(new DishwasherControlParameters() {
            @Override
            public String getProgram() {
                return dishwasher.getCurrentState().getProgram();
            }

            @Override
            public Date getStartTime() {
                return context.currentTime();
            }
        });
        return update(locale);
    }

    @Override
    public String getTitle(Locale locale) {
        return "Miele@Home Dishwasher";
    }
}

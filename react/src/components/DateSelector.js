import { DatePicker, Button } from 'antd';
import dayjs from 'dayjs';
import React, { useState, useEffect } from 'react';
import '../index.css';

const DateSelector = ({ selectedDate, onChange }) => {
  const [tempDate, setTempDate] = useState(selectedDate);

  useEffect(() => {
    setTempDate(selectedDate);
  }, [selectedDate]);

  const handleDateChange = (date) => {
    setTempDate(date); 

    if (date === null) {
        onChange(dayjs());
    }
  };

  return (
    <div style={{ display: 'flex', gap: '20px', marginBottom: '0px', fontWeight: 400, fontFamily: 'Inter, sans-serif', fontStyle: 'normal', fontSize: 24, color: '#1E1E1E'}}>
      <DatePicker 
        defaultValue={dayjs()}
        value={tempDate}
        onChange={handleDateChange}
        format="DD.MM.YYYY"
        size="large"  
      />
      <Button type="default"
        size="large"
        className="custom-button" 
        onClick={() => onChange(tempDate)}>
            Choose day
      </Button>
    </div>
  );
};

export default DateSelector;

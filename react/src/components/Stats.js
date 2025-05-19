const Stats = ({ stats }) => {
  const { calories, proteins, fats, carbs } = stats;
  return (
    <div style={{ display: 'flex', gap: '20px', marginBottom: '10px' }}>
      <div style={{backgroundColor: '#FAF6E9', padding: '15px', paddingBottom: '0px', borderRadius: '8px', minWidth: '110px', maxWidth: '110px', fontWeight: 500, fontFamily: 'Inter, sans-serif', fontStyle: 'normal', fontSize: 16, color: '#1E1E1E'}}>Calories <p style={{paddingTop: '10px', paddingBottom: '0px', fontWeight: 400, fontFamily: 'Inter, sans-serif', fontStyle: 'normal', fontSize: 14, color: '#1E1E1E'}}>{calories.toFixed(2)}</p></div>
      <div style={{backgroundColor: '#FAF6E9', padding: '15px', paddingBottom: '0px', borderRadius: '8px', minWidth: '110px', maxWidth: '110px', fontWeight: 500, fontFamily: 'Inter, sans-serif', fontStyle: 'normal', fontSize: 16, color: '#1E1E1E'}}>Proteins <p style={{paddingTop: '10px', paddingBottom: '0px', fontWeight: 400, fontFamily: 'Inter, sans-serif', fontStyle: 'normal', fontSize: 14, color: '#1E1E1E'}}>{proteins.toFixed(2)} g</p></div>
      <div style={{backgroundColor: '#FAF6E9', padding: '15px', paddingBottom: '0px', borderRadius: '8px', minWidth: '110px', maxWidth: '110px', fontWeight: 500, fontFamily: 'Inter, sans-serif', fontStyle: 'normal', fontSize: 16, color: '#1E1E1E'}}>Fats <p style={{paddingTop: '10px', paddingBottom: '0px', fontWeight: 400, fontFamily: 'Inter, sans-serif', fontStyle: 'normal', fontSize: 14, color: '#1E1E1E'}}>{fats.toFixed(2)} g</p></div>
      <div style={{backgroundColor: '#FAF6E9', padding: '15px', paddingBottom: '0px', borderRadius: '8px', minWidth: '110px', maxWidth: '110px', fontWeight: 500, fontFamily: 'Inter, sans-serif', fontStyle: 'normal', fontSize: 16, color: '#1E1E1E'}}>Carbs <p style={{paddingTop: '10px', paddingBottom: '0px', fontWeight: 400, fontFamily: 'Inter, sans-serif', fontStyle: 'normal', fontSize: 14, color: '#1E1E1E'}}>{carbs.toFixed(2)} g</p></div>
    </div>
  );
};

export default Stats;

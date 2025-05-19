import React, { useState, useEffect } from 'react';
import { Layout, Button, Modal, message, Skeleton } from 'antd';
import DateSelector from './components/DateSelector';
import Stats from './components/Stats';
import MealSection from './components/MealSection';
import AddProductModal from './components/AddProductModal';
import EditProductModal from './components/EditProductModal';
import { FireOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import axios from 'axios';

const { Header, Footer, Content } = Layout;

const App = () => {
  const [products, setProducts] = useState({ Breakfast: [], Lunch: [], Dinner: [] });
  const [editProduct, setEditProduct] = useState(null);
  const [addModal, setAddModal] = useState({ visible: false, meal: 'Breakfast' });
  const [selectedDate, setSelectedDate] = useState(dayjs());
  const [dayExists, setDayExists] = useState(true);
  const [dayId, setDayId] = useState(null);
  const [addingProduct, setAddingProduct] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();
  const [mealIds, setMealIds] = useState({});
  const [loading, setLoading] = useState(false);       

  const parseApiResponse = (data) => {
    const mealMapping = {
      breakfast: 'Breakfast',
      lunch: 'Lunch',
      dinner: 'Dinner',
    };

    const result = {
      Breakfast: [],
      Lunch: [],
      Dinner: [],
    };

    data.meals.forEach(meal => {
      const mappedMeal = mealMapping[meal.mealType.toLowerCase()];
      if (mappedMeal) {
        result[mappedMeal] = meal.products.map(product => ({
          id: product.id,
          name: product.name,
          weight: product.weight,
          calories: product.calories,
          proteins: product.proteins,
          fats: product.fats,
          carbs: product.carbs,
          mealId: meal.id, 
        }));
      }
    });

    return result;
  };

  useEffect(() => {
    const fetchDayData = async () => {
      try {
        setLoading(true); 
        const res = await axios.get(`http://localhost:8080/days?date=${selectedDate.format('YYYY-MM-DD')}`);
        if (res.data.length > 0) {
          setDayExists(true);
          setDayId(res.data[0].id);
          const parsedProducts = parseApiResponse(res.data[0]);
          setProducts(parsedProducts);
        } else {
          setDayExists(false);
          setDayId(null);
          setProducts({ Breakfast: [], Lunch: [], Dinner: [] });
        }
      } catch (err) {
        if (err.response?.status === 404) {
          setDayExists(false);
          setDayId(null);
          setProducts({ Breakfast: [], Lunch: [], Dinner: [] });
        } else {
          console.error(err);
        }
      }
      setLoading(false);
    };

    fetchDayData();
  }, [selectedDate]);

  const updateStats = () => {
    const all = Object.values(products).flat();
    return {
      calories: all.reduce((sum, p) => sum + p.calories, 0),
      proteins: all.reduce((sum, p) => sum + p.proteins, 0),
      fats: all.reduce((sum, p) => sum + p.fats, 0),
      carbs: all.reduce((sum, p) => sum + p.carbs, 0),
    };
  };

  const addProduct = async (meals, product) => {
    try {
      if (!meals || meals.length === 0) {
        console.error('No meals selected');
        return;
      }
      setLoading(true); 
      let currentDayId = dayId;
      const newMealIds = {};

      if (!dayExists) {
        const createDayRes = await axios.post('http://localhost:8080/days', {
          date: selectedDate.format('YYYY-MM-DD'),
          meals: meals.map(m => ({
            mealType: m.toLowerCase(),
            products: [],
          })),
        });

        currentDayId = createDayRes.data.id;
        setDayId(currentDayId);
        setDayExists(true);

        createDayRes.data.meals.forEach(m => {
          newMealIds[m.mealType.toLowerCase()] = m.id;
        });

        setMealIds(newMealIds);
      } else {
        const res = await axios.get(`http://localhost:8080/days?date=${selectedDate.format('YYYY-MM-DD')}`);
        if (res.data.length > 0) {
          const day = res.data[0];
          currentDayId = day.id;

          for (const m of meals) {
            const mealType = m.toLowerCase();
            const foundMeal = day.meals.find(meal => meal.mealType.toLowerCase() === mealType);
            if (foundMeal) {
              newMealIds[mealType] = foundMeal.id;
            } else {
              const createMealRes = await axios.post(`http://localhost:8080/days/${day.id}/meals`, {
                mealType,
                products: [],
              });
              newMealIds[mealType] = createMealRes.data; 
            }
          }
        }
      }

      const isQuery = product.query && product.query.trim() !== '';

      for (const meal of meals) {
        const mealId = newMealIds[meal.toLowerCase()];
        if (!mealId) {
          console.error(`Cannot get mealId for: ${meal}`);
          continue;
        }

        try {
          if (isQuery) {
            await axios.post(`http://localhost:8080/meals/${mealId}/products?q=${encodeURIComponent(product.query.trim())}`);
          } else {
            await axios.post(`http://localhost:8080/meals/${mealId}/products`, {
              name: product.name,
              weight: product.weight.toString(),
              calories: product.calories.toString(),
              proteins: product.proteins.toString(),
              carbs: product.carbs.toString(),
              fats: product.fats.toString(),
            });
          }
        } catch (error) {
          if (error.response?.status === 404) {
            messageApi.error(`Product was not found for the query "${product.query}"`);
            setLoading(false); 
            return; 
          } else {
            throw error; 
          }
        }
      }

      const res = await axios.get(`http://localhost:8080/days?date=${selectedDate.format('YYYY-MM-DD')}`);
      if (res.data.length > 0) {
        const parsedProducts = parseApiResponse(res.data[0]);
        setProducts(parsedProducts);
      }
    } catch (error) {
      console.error('Error adding product:', error);
    }
    setLoading(false); 
    messageApi.success('Product was added successfully');
    return; 
  };

  const deleteProduct = async (meal, productId) => {
    try {
      if (!dayId) {
        message.error('No day selected');
        return;
      }
      setLoading(true);
      const res = await axios.get(`http://localhost:8080/days?date=${selectedDate.format('YYYY-MM-DD')}`);
      if (res.data.length === 0) {
        messageApi.error('Day data not found');
        setLoading(false); 
        return;
      }
      const day = res.data[0];

      const mealObj = day.meals.find(m => m.mealType.toLowerCase() === meal.toLowerCase());
      if (!mealObj) {
        messageApi.error('Meal not found');
        setLoading(false); 
        return;
      }
      const mealId = mealObj.id;

      await axios.delete(`http://localhost:8080/meals/${mealId}/products/${productId}`);

      const updatedRes = await axios.get(`http://localhost:8080/days?date=${selectedDate.format('YYYY-MM-DD')}`);
      if (updatedRes.data.length > 0) {
        const parsedProducts = parseApiResponse(updatedRes.data[0]);
        setTimeout(() => {
          messageApi.success('Product deleted successfully');
        }, 300);
        setProducts(parsedProducts);
      } else {
        setTimeout(() => {
          messageApi.success('Product deleted successfully');
        }, 300);
        setProducts({ Breakfast: [], Lunch: [], Dinner: [] });
      }
    } catch (error) {
      console.error('Error deleting product:', error);
      messageApi.error('Failed to delete product');
      setLoading(false); 
    }
    setLoading(false); 
  };


  const handleDateChange = (date) => {
    setSelectedDate(date);  
  };

  const updateProduct = async (productId, mealId, changes) => {
    try {
      const patchData = Object.entries(changes)
      .filter(([key]) => key !== 'id' && key !== 'meal' && key !== 'mealId')
      .map(([key, value]) => ({
        op: 'replace',
        path: `/${key}`,
        value,
      }));
      const res = await axios.patch(
        `http://localhost:8080/meals/${mealId}/products/${productId}`,
        patchData,
        {
          headers: {
            'Content-Type': 'application/json-patch+json',
          },
        }
      );

      const updatedRes = await axios.get(`http://localhost:8080/days?date=${selectedDate.format('YYYY-MM-DD')}`);
      if (updatedRes.data.length > 0) {
        const parsedProducts = parseApiResponse(updatedRes.data[0]);
        setProducts(parsedProducts);
      }

      messageApi.success('Product updated successfully');
    } catch (error) {
      console.error('Error updating product:', error);
      messageApi.error('Failed to update product');
    }
  };

  const handleEditSave = async (updatedFields) => {
    setLoading(true);
    const { id, mealId, meals, ...changes } = updatedFields;

    await updateProduct(id, mealId, changes);

    if (meals && Array.isArray(meals) && meals.length > 1) {
      const originalMeal = Object.keys(products).find(meal =>
        products[meal].some(p => p.id === id)
      )?.toLowerCase();
      const newMeals = meals.filter(m => m !== originalMeal);

      for (const meal of newMeals) {
        const mealKey = meal.toLowerCase();
        const mealIdToCheck = mealIds[mealKey] || (await (async () => {
          const res = await axios.get(`http://localhost:8080/days?date=${selectedDate.format('YYYY-MM-DD')}`);
          if (res.data.length > 0) {
            const day = res.data[0];
            const foundMeal = day.meals.find(m => m.mealType.toLowerCase() === mealKey);
            if (!foundMeal) {
              const createMealRes = await axios.post(`http://localhost:8080/days/${day.id}/meals`, {
                mealType: mealKey,
                products: [],
              });
              setMealIds(prev => ({ ...prev, [mealKey]: createMealRes.data }));
              setLoading(false); 
              return createMealRes.data;
            }
            setMealIds(prev => ({ ...prev, [mealKey]: foundMeal.id }));
            setLoading(false); 
            return foundMeal.id;
          }
          setLoading(false); 
          return null;
        })());

        if (mealIdToCheck) {
          const exists = products[meal.charAt(0).toUpperCase() + meal.slice(1)]?.some(
            p => p.name.trim().toLowerCase() === changes.name.trim().toLowerCase()
          );

          if (!exists) {
            await axios.post(`http://localhost:8080/meals/${mealIdToCheck}/products`, {
              name: changes.name,
              weight: changes.weight.toString(),
              calories: changes.calories.toString(),
              proteins: changes.proteins.toString(),
              carbs: changes.carbs.toString(),
              fats: changes.fats.toString(),
            });
             messageApi.success('Product added successfully');
          }
        }
      }

      const res = await axios.get(`http://localhost:8080/days?date=${selectedDate.format('YYYY-MM-DD')}`);
      if (res.data.length > 0) {
        const parsedProducts = parseApiResponse(res.data[0]);
        setProducts(parsedProducts);
      }
    }
    setLoading(false); 
  };

return (
    <Layout style={{ backgroundColor: '#FFFDF6' }}>
      {contextHolder}
      <Header className="header" style={{ display: 'flex', alignItems: 'center', paddingLeft: 20 }}>
        <FireOutlined style={{ fontSize: 28, color: '#1E1E1E', marginRight: 8 }} />
        <span style={{ fontWeight: 800, fontFamily: 'Inter, sans-serif', fontStyle: 'normal', fontSize: 24 }}>
          Calorie Calculator
        </span>
      </Header>
      <div
        style={{
          padding: '20px',
          backgroundColor: '#FFFDF6',
          maxWidth: 800,
          margin: '0 auto',
          marginTop: '35px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'flex-start',
        }}
      >
        <DateSelector selectedDate={selectedDate} onChange={handleDateChange} />
      </div>
      <div style={{ margin: '20px auto' }}>
        <Stats stats={updateStats()} />
      </div>
      <Content
        style={{
          padding: '20px',
          paddingTop: '15px',
          paddingLeft: '15px',
          backgroundColor: '#FAF6E9',
          maxWidth: 800,
          margin: '20px auto',
          marginTop: '10px',
          borderRadius: '8px',
          minHeight: '70vh',
        }}
      >
        {loading ? (
          <div style={{ width: '100%' }}>
            <div
              style={{
                maxWidth: 1000,
                margin: '15px auto 10px auto',
                padding: '0px',
                paddingLeft: '15px',
                textAlign: 'left',
                minWidth: 600,
                paddingLeft: '15px'
              }}
            >
              <Skeleton active paragraph={{ rows: 0 }} title={{ width: '20%' }} />
            </div>
            {['Breakfast', 'Lunch', 'Dinner'].map((meal, index) => (
              <div
                key={index}
                style={{
                  marginBottom: '30px',
                  paddingLeft: '15px',
                  paddingRight: '15px',
                  minWidth: 600,
                  paddingLeft: '15px'
                }}
              >
                <Skeleton
                  active
                  paragraph={{ rows: 2, width: ['100%', '100%'] }}
                  title={{ width: '100%' }}
                />
              </div>
            ))}
          </div>
        ) : (
          <>
            <div
              style={{
                maxWidth: 1000,
                margin: '15px auto 10px auto',
                padding: '0px',
                paddingLeft: '15px',
                textAlign: 'left',
                fontSize: 20,
                color: '#1E1E1E',
                fontFamily: 'Inter, sans-serif',
                fontWeight: 600,
              }}
            >
              {selectedDate.format('DD.MM.YYYY')}
            </div>
            <div className="section">
              {['Breakfast', 'Lunch', 'Dinner'].map(meal => (
                <MealSection
                  key={meal}
                  meal={meal}
                  products={products[meal]}
                  onAdd={() => setAddModal({ visible: true, meal })}
                  onEdit={product => setEditProduct({ ...product, mealId: product.mealId })}
                  onDelete={deleteProduct}
                  loading={addingProduct}
                />
              ))}
            </div>
          </>
        )}
      </Content>
      <Footer className="footer">Copyright © 2025. All rights reserved</Footer>

      <AddProductModal
        visible={addModal.visible}
        meal={addModal.meal}
        onClose={() => setAddModal({ visible: false, meal: 'Breakfast' })}
        onAdd={addProduct}
      />

      {editProduct && (
        <EditProductModal
          product={editProduct}
          onClose={() => setEditProduct(null)}
          onSave={handleEditSave}
        />
      )}
    </Layout>
  );
};
export default App;

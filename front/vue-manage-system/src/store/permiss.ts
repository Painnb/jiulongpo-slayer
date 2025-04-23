import { defineStore } from 'pinia';

interface ObjectList {
    [key: string]: string[];
}

export const usePermissStore = defineStore('permiss', {
    state: () => {
        const defaultList: ObjectList = {
            admin: ['0', '11', '12', '13','7','8'],
            user: ['0', '11', '12', '13','7','8'],
        };
        const Auth = localStorage.getItem('auth');
        console.log(Auth);
        return {
            key: (Auth == 'BIZ_ADMIN'||'SYS_ADMIN' ? defaultList.admin : defaultList.user) as string[],
            defaultList,
        };
    },
    actions: {
        handleSet(val: string[]) {
            this.key = val;
        },
    },
});
